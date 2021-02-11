var jobUpdateHandlers = [];

onPageTransition.push(function () {
    jobUpdateHandlers = [];
});

$(function () {
    function timeoutHandler() {
        var handlers = jobUpdateHandlers;
        jobUpdateHandlers = [];
        for (let handler of handlers) handler();
        setTimeout(timeoutHandler, 5000);
    }

    setTimeout(timeoutHandler, 5000);
});

function initJob(containerSelector, collectionId, jobId, sample) {
    var $container = $(containerSelector);

    $container.find(".job-runbutton").click(function () {
        $.getJSON("/ait/api/runjob/" + jobId + "/" + collectionId + (sample ? "?sample=true" : ""), function (json) {
            $container.find(".jobstate").text(json.state);
            $container.find(".job-button").css("display", "none");
            if (!json.started) {
                $container.find(".job-runbutton").css("display", "block");
            } else if (json.finished) {
                $container.find(".job-resultsbutton").css("display", "block");
            } else {
                $container.find(".job-resultsbutton-disabled").css("display", "block");
            }
        });
    });

    function update() {
        $.getJSON("/ait/api/jobstate/" + jobId + "/" + collectionId + (sample ? "?sample=true" : ""), function (json) {
            $container.find(".jobstate").text(json.state);
            $container.find(".job-button").css("display", "none");
            if (!json.started) {
                $container.find(".job-runbutton").css("display", "block");
            } else if (json.finished) {
                $container.find(".job-resultsbutton").css("display", "block");
            } else {
                $container.find(".job-resultsbutton-disabled").css("display", "block");
            }
            jobUpdateHandlers.push(update);
        });
    }

    update();
}

