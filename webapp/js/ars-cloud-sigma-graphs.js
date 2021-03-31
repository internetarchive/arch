function zoomIn(instance) {
  var camera = instance.camera;
  sigma.misc.animation.camera(camera, {
    ratio: camera.ratio / camera.settings('zoomingRatio')
  }, {
    duration: 200
  });
}

function zoomOut(instance) {
  var camera = instance.camera;
  // Zoom out - animation :
  sigma.misc.animation.camera(camera, {
    ratio: camera.ratio * camera.settings('zoomingRatio')
  }, {
    duration: 200
  });
}

function refresh(instance) {
  var camera = instance.camera;
  sigma.misc.animation.camera(camera, {
    ratio: 1,
    x: 0,
    y: 0
  }, {
    duration: 200
  });
}

function goFullScreen() {
  var elem = document.documentElement;
  if (elem.requestFullscreen) {
    elem.requestFullscreen();
  } else if (elem.msRequestFullscreen) {
    elem.msRequestFullscreen();
  } else if (elem.mozRequestFullScreen) {
    elem.mozRequestFullScreen();
  } else if (elem.webkitRequestFullscreen) {
    elem.webkitRequestFullscreen();
  }
}

function leaveFullScreen() {
  if (document.exitFullscreen) {
    document.exitFullscreen();
  } else if (document.webkitExitFullscreen) {
    document.webkitExitFullscreen();
  } else if (document.mozCancelFullScreen) {
    document.mozCancelFullScreen();
  } else if (document.msExitFullscreen) {
    document.msExitFullscreen();
  }
}

$(document).on(function () {
  var s;
  var gm;
  s = new sigma({ // eslint-disable-line new-cap
    renderers: [
      {
        container: document.getElementById('graph'),
        type: 'canvas' // sigma.renderers.canvas works as well
      }]
  });
  gm = new sigma({ // eslint-disable-line new-cap
    renderers: [
      {
        container: document.getElementById('graph-modal'),
        type: 'canvas'
      }]
  });

  // resize graph-modal if the window changes
  $(window).on('resize', function () {
    $('div#graph-modal').height($(window).height() * 0.83);
  });

  $('.zoom-in').on('click', function () {
    zoomIn(gm);
    zoomIn(s);
  });

  $('.zoom-out').on('click', function () {
    zoomOut(gm);
    zoomOut(s);
  });

  $('.default').on('click', function () {
    refresh(gm);
    refresh(s);
    $('.scale-down').prop('disabled', true);
    state = 0;
  });

  $('#image-link').on('click', function () {
    var button = document.getElementById('image-link');
    var canvas = $('.sigma-scene');
    var camera = s.camera;
    var fn = button.getAttribute('download').replace('-image.png', '');
    var img = canvas[1].toDataURL('image/png');
    button.setAttribute('download', fn + 'xyr-' + Math.abs(Math.round(camera.x))
      + '-' + Math.abs(Math.round(camera.y)) + '-' + Math.abs(Math.round(camera.ratio))
      + '-image.png');
    button.href = img;
  });

  $('#modal-image-link').on('click', function () {
    var button = document.getElementById('modal-image-link');
    var canvas = $('.sigma-scene');
    var camera = gm.camera;
    var fn = button.getAttribute('download').replace('-image.png', '');
    var img = canvas[0].toDataURL('image/png');
    button.setAttribute('download', fn + 'xyr-' + Math.abs(Math.round(camera.x))
      + '-' + Math.abs(Math.round(camera.y)) + '-' + Math.abs(Math.round(camera.ratio))
      + '-image.png');
    button.href = img;
  });

  $('span#modal-click').on('click', function () {
    goFullScreen();
  });

  $('button#modal-exit-fullscreen').on('click', function () {
    leaveFullScreen();
  });

  // display sigma when modal is launched.
  $('body').on('shown.bs.modal', function () {
    gm.renderers[0].resize();
    gm.refresh();
  });

  // remove sigma on hidden modal.
  $('body').on('hidden.bs.modal', function () {
    $('div#graph-modal canvas').html('');
  });
});
