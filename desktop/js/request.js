const fields = [["c", "country"], ["st", "state"], ["l", "locality"], ["o", "organization"], ["ou", "organizationalUnit"], ["cn", "commonName"]];

let required = null;

function init() {
    ipcRenderer.on("ca-params", (event, args) => {
        required = args;
        for (let f of fields) {
            if (args[f[1]]) {
                document.getElementById(f[0]).placeholder += " (obvezno)";
            }
        }
    });

    ipcRenderer.invoke("ca-params", null);
}

function send() {
    let data = {
        // contact date
        name: $("#name").val(),
        surname: $("#surname").val(),
        email: $("#email").val(),
        // cert data
        country: $("#c").val(),
        state: $("#st").val(),
        locality: $("#l").val(),
        organization: $("#o").val(),
        organizationalUnit: $("#ou").val(),
        commonName: $("#cn").val(),
    }
    // validate contact data input
    let proceed = true;
    for (let id of ["name", "surname", "email"]) {
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
    // validate cert data
    for (let id of fields) {
        if (required[id[1]]) {
            let elem = document.getElementById(id[0]);
            let valid = elem.reportValidity();
            if (!valid) {
                proceed = false;
                break;
            }
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

init();