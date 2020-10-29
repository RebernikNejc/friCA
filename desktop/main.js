const electron = require("electron");
const {app, dialog, ipcMain, BrowserWindow} = require("electron");
const Request = require("request");
const {exec, execSync} = require("child_process");
const fs = require("fs");
const NodeRSA = require("node-rsa");

const HOST = "http://127.0.0.1:8443";

let params = null;

function init() {
    ipcMain.handle("open", (event, args) => {
        switch (args) {
            case "request":
                win.loadFile("./html/request.html");
                break;
            case "deliver":
                win.loadFile("./html/deliver.html");
                break;
            case "progress":
                win.loadFile("./html/progress.html");
                break;
        }
    });

    ipcMain.handle("cancel", (event, args) => {
        win.loadFile("./html/index.html");
    });

    ipcMain.handle("ca-params", (event, args) => {
        Request.get({
            url: HOST + "/ca-params"
        }, (error, response, body) => {
            event.sender.send("ca-params", JSON.parse(body));
        });
    });

    ipcMain.handle("request", (event, args) => {
        Request.get({
            url: HOST + "/ca-params"
        }, (error, response, body) => {
            if (error) {
                console.log(error);
                return;
            }
            let j = JSON.parse(body);
            let time = Date.now();
            // generate private key and csr
            let command = createCsrCommand(j.keySize, time, args.country, args.state, args.locality, args.organization, args.organizationalUnit, args.commonName);

            console.log(command);

            execSync(command);
            // make request on API
            console.log(args);
            let req = Request.post({
                url: HOST + "/csr",
                headers: {
                    "name": args.name,
                    "surname": args.surname,
                    "email": args.email
                }
            }, (error, response, body) => {
                if (error) {
                    console.log(error);
                    // show fail page
                    win.loadURL(__dirname + "/html/request-fail.html");
                    return;
                }
                let g = JSON.parse(body);
                console.log(g);
                // rename file
                fs.renameSync(time + ".csr", g.id + ".csr");
                fs.renameSync(time + ".key", g.id + ".key");
                fs.writeFileSync(g.id + ".token", g.encryptedToken);
                // show confirmation page with info
                win.loadURL(__dirname + "/html/request-success.html?id=" + g.id);
            });
            req.form().append("csr", fs.createReadStream(time + ".csr"));
        });
    });

    ipcMain.handle("progress", (event, args) => {
        // find suitable requests
        let csrs = new Set();
        let keys = new Set();
        let tokens = new Set();
        // find .csr, .key and .token triplets and list them
        let files = fs.readdirSync(".");
        for (let file of files) {
            console.log(file);
            let s = file.split(".");
            if (s.length == 2) {
                if (s[1] == "csr") {
                    csrs.add(s[0]);
                }
                switch (s[1]) {
                    case "csr":
                        csrs.add(s[0]);
                        break;
                    case "key":
                        keys.add(s[0]);
                        break;
                    case "token":
                        tokens.add(s[0]);
                        break;
                }
            }
        }
        let valid = new Set([...csrs].filter(x => keys.has(x)));
        valid = new Set([...valid].filter(x => tokens.has(x)));
        console.log(valid);

        let loading = valid.size;
        let results = [];

        for (let id of [...valid].reverse()) {
            console.log(id);
            // compute token challenge with NodeRSA
            let r = fs.readFileSync(id + ".token").toString();
            let pk = new NodeRSA(fs.readFileSync(id + ".key").toString(), "pkcs8-private", {
                encryptionScheme: "pkcs1"
            });
            let t = pk.decrypt(r, "base64");
            console.log(loading);
            let req = Request.get({
                url: HOST + "/csr",
                headers: {
                    id: id,
                    token: t
                }
            }, (error, response, body) => {
                console.log("callback");
                if (error) {
                    console.log(error);
                    return;
                }
                results.push(JSON.parse(body));
                loading--;
                console.log(loading);
                // if loading is finished show page
                if (loading == 0) {
                    console.log("done");
                    event.sender.send("statuses", results);
                }
            });
        }
    });

    ipcMain.handle("deliver", (event, args) => {
        let privateKey = new NodeRSA();
        privateKey.setOptions({
            encryptionScheme: "pkcs1"
        });
        privateKey.importKey(fs.readFileSync(args.id + ".key").toString(), "pkcs8-private");

        let decrypted = private.decrypt(tokfs.readFileSync(args.id + ".token").toString(), "base64");
        
        let req = Request.get({
            url: HOST + "/csr/crt",
            headers: {
                id: args.id,
                token: decrypted
            },
            rejectUnauthorized: false
        }, (error, response, body) => {
            if (error) {
                console.log(error);
                return;
            }
            console.log(body);
            // save content to file, then join new crt and key to create .p12
            fs.writeFileSync(args.id + ".crt", body);
            // execure openssl to create .p12
            let command = "openssl pkcs12 -export -out " + args.id + ".p12 -inkey " + args.id + ".key -in " + args.id + ".crt -passin pass:password -passout pass:" + args.pass;
            console.log(command);
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    console.log(error);
                    console.log(stderr);
                    return;
                }
                console.log(stdout);
            });
        });
    });

    const win = new BrowserWindow({
        width: 1092,
        height: 614,
        webPreferences: {
            nodeIntegration: true
        }
    });
    // win.setMenuBarVisibility(false)
    win.loadFile("./html/index.html");
}

function createCsrCommand(keySize, filename, country, state, locality, organization, organizationalUnit, commonName) {
    return 'openssl req -new -newkey rsa:' + keySize + ' -nodes -keyout ' + filename + '.key -out ' + filename + '.csr -subj "/C=' + country + '/ST=' + state + '/L=' + locality + '/O='+ organization + '/OU=' + organizationalUnit + '/CN=' + commonName + '"';
}

app.whenReady().then(init);