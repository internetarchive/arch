var arch = (function () {
    var jobUpdateHandlers = [];
    var activeRequests = {};
    var timeoutSet = false;
    var BASE_PATH = "";

    function timeoutHandler() {
        for (let handler of jobUpdateHandlers) handler();
        timeoutSet = false;
        requestInterval();
    }

    function requestInterval() {
        if (!timeoutSet && Object.keys(activeRequests).length === 0) {
            timeoutSet = true;
            setTimeout(timeoutHandler, 5000);
        }
    }

    function finishRequest(url) {
        if (url) delete activeRequests[url];
        requestInterval();
    }

    $(requestInterval);

    function loadCollectionInfo(containerSelector, collectionId, userCollectionId) {
        var $container = $(containerSelector);
        var $lastJobNameCell = $container.find(".collection-lastjob-name");
        var $lastJobTimeCell = $container.find(".collection-lastjob-time");
        var $sizeCell = $container.find(".collection-size");
        var $seedsCell = $container.find(".collection-seeds");
        var $lastCrawlDateCell = $container.find(".collection-last-crawl-date");

        var url = BASE_PATH + "/api/collection/" + collectionId;
        activeRequests[url] = $.getJSON(url, function (json) {
            if (json.lastJobName) {
                var lastJobUrl = BASE_PATH + "/" + userCollectionId + "/analysis/" + json.lastJobId + (json.lastJobSample ? "?sample=true" : "");
                var $lastJobLink = $("<a>").attr("href", lastJobUrl).text(json.lastJobName);
                $lastJobNameCell.empty().append($lastJobLink);
            } else {
                $lastJobNameCell.text("-");
            }
            $lastJobTimeCell.text(json.lastJobTime || "-");
            $sizeCell.text(json.size);
            $sizeCell.attr("sorttable_customkey", json.sortSize);
            $seedsCell.text(json.seeds);
            var lastCrawlDate = new Date(json.lastCrawlDate);
            $lastCrawlDateCell.text(lastCrawlDate.toLocaleDateString('en-us', {year:"numeric", month:"short", day:"numeric"}));

            finishRequest(url);
        });
    }

    function refreshJobTables(collectionId, userCollectionId, processTable, completedTable) {
        var $processTable = $(processTable).children("tbody");
        var $completedTable = $(completedTable).children("tbody");

        var $td1Template = $("<td>").css("font-style", "italic");
        var $td2Template = $("<td>");
        var $rowTemplate = $("<tr>").append([$td1Template, $td2Template]);

        jobUpdateHandlers.push(function () {
            let url = BASE_PATH + "/api/jobstates/" + collectionId + "?all=true";
            activeRequests[url] = $.getJSON(url, function (data) {
                let runningJobs = 0;
                let finishedJobs = 0;

                $processTable.empty();
                $completedTable.empty();
                for (let job of data) {
                    if (job.started && !job.failed) {
                        let isSample = job.sample && job.sample >= 0
                        $td1Template.empty();
                        if (job.finished) {
                            finishedJobs += 1;
                            $rowTemplate.attr("class", "finished-tr");
                            $td2Template.attr("class", "finished-td-finished").text(job.finishedTime);
                            let url = BASE_PATH + "/" + userCollectionId + "/analysis/" + job.id
                            if (isSample) {
                                $td1Template.append($("<a>").attr("href", url + "?sample=true").text(job.name + " (Example)"));
                            } else {
                                $td1Template.append($("<a>").attr("href", url).text(job.name));
                            }
                            $completedTable.append($rowTemplate.clone());
                        } else {
                            runningJobs += 1;
                            $rowTemplate.attr("class", "running-tr");
                            $td2Template.attr("class", "running-td-state").text(job.queue ? job.queue + " #" + (job.queuePos + 1) : job.activeState);
                            if (isSample) {
                                $td1Template.text(job.name + " (Example)");
                            } else {
                                $td1Template.text(job.name);
                            }
                            $processTable.append($rowTemplate.clone());
                        }
                    }
                }

                $("#summary-loading").hide();
                $("#summary-empty").toggle(runningJobs === 0);
                $("#summary-running").toggle(runningJobs > 0);
                $("#summary-finished").toggle(finishedJobs > 0);

                finishRequest(url);
            });
        });
    }

    function refreshJobCards(collectionId) {
        var cardsInitialized = {};

        function updateState(job) {
            let isSample = job.sample && job.sample >= 0
            var $card = $("#card-" + job.id).find(isSample ? ".job-card-sample" : ".job-card-full");
            var cardId = job.id + (isSample ? "-sample" : "");
            if (!cardsInitialized[cardId]) {
                cardsInitialized[cardId] = true;
                $card.find(".job-runbutton").click(function () {
                    let url = BASE_PATH + "/api/runjob/" + job.id + "/" + collectionId + (isSample ? "?sample=true" : "");
                    activeRequests[url] = $.getJSON(url, function (json) {
                        updateState(json);
                        finishRequest(url);
                    });
                });
            }
            $card.find(".job-button").css("display", "none");
            if (!job.started) {
                $card.find(".job-runbutton").css("display", "block");
            } else if (job.finished) {
                $card.find(".job-resultsbutton").css("display", "block");
            } else {
                var $stateButton = $card.find(".job-statebutton");
                $stateButton.text(job.state);
                $stateButton.css("display", "block");
            }
        }

        jobUpdateHandlers.push(function () {
            let url = BASE_PATH + "/api/jobstates/" + collectionId + "?all=true";
            activeRequests[url] = $.getJSON(url, function (data) {
                for (let job of data) {
                    updateState(job);
                }

                finishRequest(url);
            });
        });
    }

    function switchTab(id) {
        $(".subnav-link").removeClass("active");
        $(".page-tab").hide();
        $("#" + id).show();
        $("#" + id + "-link").addClass("active");
        window.scrollTo(0,0);
        return false;
    }

    function registerTab(id) {
        $("#" + id + "-link").click(function() {
            return switchTab(id);
        });
    }

    return {
        loadCollectionInfo: loadCollectionInfo,
        refreshJobTables: refreshJobTables,
        refreshJobCards: refreshJobCards,
        registerTab: registerTab,
        switchTab: switchTab
    };
})();
