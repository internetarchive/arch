function initJob(rowId, collectionId, jobId) {
    var $row = $("#" + rowId);

    $row.find(".job-runbutton").click(function () {
        $.getJSON("/ait/api/runjob/" + jobId + "/" + collectionId, function (json) {
            $row.find(".jobstate").text(json.state);
            $row.find(".job-button").css("display", "none");
            if (!json.started) {
                $row.find(".job-runbutton").css("display", "block");
            } else if (json.finished) {
                $row.find(".job-resultsbutton").css("display", "block");
            } else {
                $row.find(".job-resultsbutton-disabled").css("display", "block");
            }
        });
    });

    function update() {
        $.getJSON("/ait/api/jobstate/" + jobId + "/" + collectionId, function (json) {
            $row.find(".jobstate").text(json.state);
            $row.find(".job-button").css("display", "none");
            if (!json.started) {
                $row.find(".job-runbutton").css("display", "block");
            } else if (json.finished) {
                $row.find(".job-resultsbutton").css("display", "block");
            } else {
                $row.find(".job-resultsbutton-disabled").css("display", "block");
            }
        });
    }

    setInterval(function () {
        update();
    }, 5000);

    update();
}