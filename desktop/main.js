const electron = require("electron");
const {app, dialog, ipcMain, BrowserWindow} = require("electron");
const Request = require("request");
const {exec, execSync} = require("child_process");
const fs = require("fs");

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

    ipcMain.handle("request", (event, args) => {
        // generate private key and csr
        let time = Date.now();
        let command = 'openssl req -new -newkey rsa:4096 -nodes -keyout ' + time + '.key -out ' + time + '.csr -subj "/C=SI/ST=/L=/O=/OU=/CN=' + args.name + ' ' + args.surname + '"';
        console.log(command);
        execSync(command);
        // make request on API
        console.log(args);
        let req = Request.post({
            url: "https://ec2-18-156-177-102.eu-central-1.compute.amazonaws.com:8443/csr",
            headers: {
                "name": args.name,
                "surname": args.surname,
                "email": args.email,
                "country": args.country,
                "enrollmentId": args.enrollmentId
            },
            rejectUnauthorized: false
        }, (error, response, body) => {
            if (error) {
                console.log(error);
                // show fail page
                win.loadURL(__dirname + "/html/request-fail.html");
                return;
            }
            let g = JSON.parse(body);
            // rename file
            fs.renameSync(time + ".csr", g.id + ".csr");
            fs.renameSync(time + ".key", g.id + ".key");
            // show confirmation page with info
            win.loadURL(__dirname + "/html/request-success.html?id=" + g.id + "&token=" + g.token);
        });
        req.form().append("csr", fs.createReadStream(time + ".csr"));
    });

    ipcMain.handle("progress", (event, args) => {
        let req = Request.get({
            url: "https://ec2-18-156-177-102.eu-central-1.compute.amazonaws.com:8443/csr?id=" + args.id + "&token=" + args.token,
            rejectUnauthorized: false
        }, (error, response, body) => {
            if (error) {
                console.log(error);
                // show error message
                event.sender.send("progress-status", "ERROR");
                return;
            }
            let g = JSON.parse(body);
            console.log(g);
            event.sender.send("progress-status", g.status);
        });
    });

    ipcMain.handle("deliver", (event, args) => {
        // let link = "https://127.0.0.1:8443/csr/crt?id=" + args.id + "&token=" + args.token;
        let link = "https://ec2-18-156-177-102.eu-central-1.compute.amazonaws.com:8443/csr/crt?id=" + args.id + "&token=" + args.token;
        console.log(link);
        let req = Request.get({
            url: link,
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
        width: 800,
        height: 600,
        webPreferences: {
            nodeIntegration: true
        }
    });
    // win.setMenuBarVisibility(false)
    win.loadFile("./html/index.html");
}

app.whenReady().then(init);