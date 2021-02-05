function initJob(cardId, collectionId, jobId) {
    var $card = $("#" + cardId);

    $card.find(".job-runbutton").click(function () {
        $.getJSON("/ait/api/runjob/" + jobId + "/" + collectionId, function (json) {
            $card.find(".jobstate").text(json.state);
            $card.find(".job-button").css("display", "none");
            if (!json.started) {
                $card.find(".job-runbutton").css("display", "block");
            } else if (json.finished) {
                $card.find(".job-resultsbutton").css("display", "block");
            } else {
                $card.find(".job-resultsbutton-disabled").css("display", "block");
            }
        });
    });

    function update() {
        $.getJSON("/ait/api/jobstate/" + jobId + "/" + collectionId, function (json) {
            $card.find(".jobstate").text(json.state);
            $card.find(".job-button").css("display", "none");
            if (!json.started) {
                $card.find(".job-runbutton").css("display", "block");
            } else if (json.finished) {
                $card.find(".job-resultsbutton").css("display", "block");
            } else {
                $card.find(".job-resultsbutton-disabled").css("display", "block");
            }
        });
    }

    setInterval(function () {
        update();
    }, 5000);

    update();
}
