const fs = require("fs");
const { request } = require("http");
const NodeRSA = require("node-rsa");

function init() {
    // register callback
    ipcRenderer.on("statuses", (event, args) => {
        console.log(args);
        for (let req of args) {
            // populate list
            let li = document.createElement("li");
            li.classList.add("list-group-item");
            li.innerHTML = "<h6>" + req.name + " " + req.surname + "</h6>Status: " + req.status + "<br><small class='text-muted'>ID zahtevka: " + req.id + "</small>";
            document.getElementById("requests").appendChild(li);
        }
        // hide spinner show list
        document.getElementById("loading").classList.add("d-none");
        document.getElementById("requests").classList.remove("d-none");
    });
    // make call to main to execute business logic
    ipcRenderer.invoke("progress");
}

init();