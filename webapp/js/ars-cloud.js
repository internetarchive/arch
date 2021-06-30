var arsCloud = (function () {
    var activeRequests = {};
    var jobUpdateHandlers = [];

    var runningJobs = 0;
    var finishedJobs = 0;

    onPageTransition.push(function () {
        jobUpdateHandlers = [];
        for (const request of Object.values(activeRequests)) request.abort();
        activeRequests = {};
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

    function loadCollectionInfo(containerSelector, collectionId) {
        var $container = $(containerSelector);
        var $lastJobNameCell = $container.find(".collection-lastjob-name");
        var $lastJobTimeCell = $container.find(".collection-lastjob-time");
        var $sizeCell = $container.find(".collection-size");

        var url = "/ait/api/collection/" + collectionId;
        activeRequests[url] = $.getJSON(url, function (json) {
            $lastJobNameCell.text(json.lastJobName || "-");
            $lastJobTimeCell.text(json.lastJobTime || "-");
            $sizeCell.text(json.size);
        });
    }

    function initJob(collectionId, jobId, sample) {
        var prevState;

        var $card = $("#card-" + jobId).find(sample ? ".job-card-sample" : ".job-card-full");
        var $runningTr = $("#running-tr-" + jobId + "-" + (sample ? "sample" : "full"));
        var $finishedTr = $("#finished-tr-" + jobId + "-" + (sample ? "sample" : "full"));

        var $buttons = $card.find(".job-button");
        var $runButton = $card.find(".job-runbutton");
        var $stateButton = $card.find(".job-statebutton");
        var $resultsButton = $card.find(".job-resultsbutton");

        var isRunning = false;
        var isFinished = false;

        function updateState(json) {
            if (json.activeState !== prevState) {
                prevState = json.activeState;
                if (isRunning) runningJobs--;
                if (isFinished) finishedJobs--;
                isRunning = false;
                isFinished = false;
                if (json.started && !json.failed) {
                    if (json.finished) {
                        isFinished = true;
                        finishedJobs++;
                        $runningTr.hide();
                        $finishedTr.show();
                        $finishedTr.children(".finished-td-finished").text(json.finishedTime);
                    } else {
                        isRunning = true;
                        runningJobs++;
                        $runningTr.show();
                        $finishedTr.hide();
                    }
                } else {
                    $runningTr.hide();
                    $finishedTr.hide();
                }
            }

            $("#summary-empty").toggle(runningJobs === 0);
            $("#summary-running").toggle(runningJobs > 0);
            $("#summary-finished").toggle(finishedJobs > 0);

            if (json.started && !json.finished && !json.failed) {
                $runningTr.children(".running-td-active-stage").text(json.activeStage);
                $runningTr.children(".running-td-state").text(json.queue ? json.queue + " #" + json.queuePos : json.activeState);
            }

            $stateButton.text(json.state);
            $buttons.css("display", "none");
            if (!json.started) {
                $runButton.css("display", "block");
            } else if (json.finished) {
                $resultsButton.css("display", "block");
            } else {
                $stateButton.css("display", "block");
            }

            jobUpdateHandlers.push(update);
        }

        $runButton.click(function () {
            $runningTr.show();
            $("#summary-running").show();
            switchTabSummary();

            var url = "/ait/api/runjob/" + jobId + "/" + collectionId + (sample ? "?sample=true" : "");
            activeRequests[url] = $.getJSON(url, updateState);
        });

        function update() {
            var url = "/ait/api/jobstate/" + jobId + "/" + collectionId + (sample ? "?sample=true" : "");
            activeRequests[url] = $.getJSON(url, updateState);
        }

        update();
    }

    return {
        loadCollectionInfo: loadCollectionInfo,
        initJob: initJob
    };
})();
