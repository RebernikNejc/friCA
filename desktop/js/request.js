function send() {
    // get data field values
    let data = {
        name: $("#name").val(),
        surname: $("#surname").val(),
        email: $("#email").val(),
        enrollmentId: $("#enrollmentId").val(),
        country: $("#country").val()
    }
    
    ipcRenderer.invoke("request", data);
}