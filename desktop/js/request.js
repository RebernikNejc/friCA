function send() {
    // get data field values
    let data = {
        name: $("#name").val(),
        surname: $("#surname").val(),
        email: $("#email").val(),
        enrollmentId: $("#enrollmentId").val(),
        country: $("#country").val()
    }
    // validate input
    let proceed = true;
    for (let id of ["name", "surname", "email", "enrollmentId", "country"]) {
        let elem = document.getElementById(id);
        let valid = elem.reportValidity();
        if (!valid) {
            proceed = false;
            break;
        }
    }
    if (!proceed) {
        return;
    }
    console.log(data);
    // hide button and show loading
    document.getElementById("ss").classList.add("d-none");
    document.getElementById("loading").classList.remove("d-none");
    // send request
    ipcRenderer.invoke("request", data);
}