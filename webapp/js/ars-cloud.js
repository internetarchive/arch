var arsCloud = (function () {
    var activeRequests = {};
    var jobUpdateHandlers = [];

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

    function initJob(containerSelector, collectionId, jobId, sample) {
        var $container = $(containerSelector);
        var $buttons = $container.find(".job-button");
        var $runButton = $container.find(".job-runbutton");
        var $stateButton = $container.find(".job-statebutton");
        var $resultsButton = $container.find(".job-resultsbutton");

        function updateState(json) {
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
