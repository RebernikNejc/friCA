const {ipcRenderer} = require("electron");
const queryString = require("query-string");

function cancel() {
    ipcRenderer.invoke("cancel");
}

function page(name) {
    ipcRenderer.invoke("open", name);
}

function getQueryParams() {
    console.log("query params");
    console.log(window.location.search);
    let qp = queryString.parse(window.location.search);
    console.log(qp);
    return qp;
}