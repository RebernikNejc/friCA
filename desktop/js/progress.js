function check(event) {
    let data = {
        id: $("#id").val(),
        token: $("#token").val()
    };
    ipcRenderer.invoke("progress", data);
    ipcRenderer.on("progress-status", (event, args) => {
        if (["Requested", "Approved", "Delivered", "Rejected"].indexOf(args) != -1) {
            document.getElementById("status").className = "alert alert-info";
            document.getElementById("status").innerHTML = "Status: " + args;
        } else {
            document.getElementById("status").className = "alert alert-danger";
            document.getElementById("status").innerHTML = "ERROR";
        }
        document.getElementById("status").style.display = "";
    });
    return false;
}