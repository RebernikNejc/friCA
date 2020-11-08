function init() {
    // register callback
    ipcRenderer.on("statuses", (event, args) => {
        console.log(args);
        // sort by id descending
        args.sort(cmp);
        // clear existing content (refresh case)
        document.getElementById("requests").innerHTML = "";
        for (let req of args) {
            // populate list
            let li = document.createElement("li");
            li.classList.add("list-group-item");
            li.innerHTML = "<h6>" + req.name + " " + req.surname + "</h6>Status: " + req.status + "<br><small class='text-muted'>ID zahtevka: " + req.id + "</small>";
            document.getElementById("requests").appendChild(li);
        }
        // hide spinner show list
        hideLoading();
    });
    // make call to main to execute business logic
    ipcRenderer.invoke("progress");
}

function deliver() {
    let data = {
        id: $("#id").val(),
        token: $("#token").val(),
        pass: $("#pass").val()
    }
    ipcRenderer.invoke("deliver", data);
}

function showLoading() {
    document.getElementById("requests").classList.add("d-none");
    document.getElementById("loading").classList.remove("d-none");
}

function hideLoading() {
    document.getElementById("loading").classList.add("d-none");
    document.getElementById("requests").classList.remove("d-none");
}

function cmp(a, b) {
    if (a.id < b.id) {
        return 1;
    }
    if (a.id > b.id) {
        return -1;
    }
    return 0;
}

init();