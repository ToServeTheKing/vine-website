/*
 * Arrows and dots for the product cards that have more than one photo.
 *
 * This is the only JavaScript the public site loads, and the site works without it: each gallery is a
 * scroll-snap strip, so the photos are already swipeable on a phone and scrollable with a trackpad. What
 * this adds is the two chevrons and the row of dots the site has always had — controls that need script
 * to do anything, which is why they are created here rather than rendered in the template and left dead
 * for anyone whose JavaScript did not load.
 *
 * Tailwind finds the class names below because src/main/tailwind/site.css lists this directory as a
 * @source. Write them out in full — a class assembled from pieces at runtime would not be in the CSS.
 */
(function () {
  'use strict';

  var ARROW =
    'absolute top-1/2 -translate-y-1/2 grid place-items-center h-11 w-11 rounded-full bg-bakery-900/40 ' +
    'text-white backdrop-blur-sm transition hover:bg-bakery-900/60 focus:outline-none focus-visible:ring-2 ' +
    'focus-visible:ring-white';

  function chevron(direction) {
    var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('class', 'h-5 w-5');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.setAttribute('aria-hidden', 'true');
    var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', direction === 'left' ? 'M15 18l-6-6 6-6' : 'M9 18l6-6-6-6');
    svg.appendChild(path);
    return svg;
  }

  function enhance(strip) {
    var photos = strip.querySelectorAll('img');
    if (photos.length < 2) return;

    var frame = strip.parentElement;
    var name = photos[0].getAttribute('alt') || 'this item';

    // Which photo is showing: whichever one's left edge is nearest the strip's scroll position. Read
    // from the scroll position rather than tracked in a variable, so a swipe and an arrow press can
    // never disagree about where we are.
    function current() {
      return Math.round(strip.scrollLeft / strip.clientWidth);
    }

    function show(index) {
      var target = Math.max(0, Math.min(photos.length - 1, index));
      strip.scrollTo({ left: target * strip.clientWidth, behavior: 'smooth' });
    }

    function arrow(direction, label, position) {
      var button = document.createElement('button');
      button.type = 'button';
      button.className = ARROW + ' ' + position;
      button.setAttribute('aria-label', label);
      button.appendChild(chevron(direction));
      button.addEventListener('click', function () {
        // Wrapping, as the old carousel did: past the last photo you land back on the first.
        var next = current() + (direction === 'left' ? -1 : 1);
        if (next < 0) next = photos.length - 1;
        if (next > photos.length - 1) next = 0;
        show(next);
      });
      return button;
    }

    frame.appendChild(arrow('left', 'Previous photo of ' + name, 'left-2'));
    frame.appendChild(arrow('right', 'Next photo of ' + name, 'right-2'));

    var dots = document.createElement('div');
    dots.className = 'absolute inset-x-0 bottom-3 flex justify-center gap-1.5';
    var buttons = [];
    photos.forEach(function (_photo, index) {
      var dot = document.createElement('button');
      dot.type = 'button';
      dot.setAttribute(
        'aria-label',
        'Show photo ' + (index + 1) + ' of ' + photos.length + ' of ' + name,
      );
      dot.addEventListener('click', function () {
        show(index);
      });
      buttons.push(dot);
      dots.appendChild(dot);
    });
    frame.appendChild(dots);

    function paint() {
      var active = current();
      buttons.forEach(function (dot, index) {
        dot.className =
          'h-1.5 rounded-full shadow-xs transition-all motion-reduce:transition-none focus:outline-none ' +
          'focus-visible:ring-2 focus-visible:ring-white ' +
          (index === active ? 'w-4 bg-white' : 'w-1.5 bg-white/60 hover:bg-white/80');
        if (index === active) {
          dot.setAttribute('aria-current', 'true');
        } else {
          dot.removeAttribute('aria-current');
        }
      });
    }

    // Keyboard: the strip is focusable, so left/right work once it has focus.
    strip.tabIndex = 0;
    strip.setAttribute('role', 'group');
    strip.addEventListener('keydown', function (event) {
      if (event.key === 'ArrowLeft') {
        event.preventDefault();
        show(current() - 1);
      }
      if (event.key === 'ArrowRight') {
        event.preventDefault();
        show(current() + 1);
      }
    });

    var scheduled = false;
    strip.addEventListener(
      'scroll',
      function () {
        // A smooth scroll fires this dozens of times; repaint once per frame.
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(function () {
          scheduled = false;
          paint();
        });
      },
      { passive: true },
    );

    paint();
  }

  document.querySelectorAll('.gallery').forEach(enhance);
})();
