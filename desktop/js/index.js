function request() {
    ipcRenderer.invoke("open", "request");
}

function deliver() {
    ipcRenderer.invoke("open", "deliver");
}

function progress() {
    ipcRenderer.invoke("open", "progress");
}