function deliver() {
    let data = {
        id: $("#id").val(),
        token: $("#token").val(),
        pass: $("#pass").val()
    }
    ipcRenderer.invoke("deliver", data);
}