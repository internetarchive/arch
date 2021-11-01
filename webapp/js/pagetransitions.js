var onPageTransition = [];

$(function () {
    var currentTransition = {};

    function transition(url, popstate) {
        for (let event of onPageTransition) event();
        var transitionId = {};
        currentTransition = transitionId;
        $.get(url, function (data) {
            if (currentTransition === transitionId) {
                var afterHead = (data.match(/<\/head>.*/s) || [data])[0];
                var $newBody = $(afterHead.match(/<body.*<\/body>/s)[0]);
                var titleMatch = data.match(/<title>(.*?)<\/title>/s);
                initPageTransitions($newBody);
                $("body").empty().append($newBody);
                var title = titleMatch ? titleMatch[1] : document.title;
                $("title").html(title);
                document.body.scrollTop = 0;
                document.documentElement.scrollTop = 0;
                if (!popstate) history.pushState({}, title, url);
            }
        });
    }

    function initPageTransitions($body) {
        $body.find("a").filter(function (i, el) {
            var $el = $(el);
            return ($el.attr("href") || "").startsWith("/") && ($el.attr("target") || "") !== "_blank";
        }).on("click", function () {
            transition($(this).attr("href"));
            return false;
        });
    }

    window.onpopstate = function(event) {
        transition(document.location, true);
    };

    initPageTransitions($("body"));
});