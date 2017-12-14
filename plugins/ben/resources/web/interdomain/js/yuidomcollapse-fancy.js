/*
  YUIDomCollapse fancy add-on by Christian Heilmann
  Version 1.0 / May 2007
  License: http://creativecommons.org/licenses/by/3.0/
  Homepage: http://onlinetools.org/tools/yuidomcollapse/
 */
// make sure the main script is available
if (YAHOO && YAHOO.otorg && YAHOO.otorg.DomCollapse && YAHOO.util && YAHOO.util.Anim) {
    // override toggle()
    YAHOO.otorg.DomCollapse.toggle = function(e) {
        function hideAll(e) {
            // hide all triggers
            for (var i = 0, j = YAHOO.otorg.DomCollapse.coanchors.length; i < j; i++) {
                if (e === YAHOO.otorg.DomCollapse.coanchors[i])
                    continue;
                // get parent
                var parent = YAHOO.util.Dom.hasClass(YAHOO.otorg.DomCollapse.coanchors[i], css.triggerClass) ? YAHOO.otorg.DomCollapse.coanchors[i] : YAHOO.otorg.DomCollapse.coanchors[i].parentNode;
                var id = YAHOO.otorg.DomCollapse.coanchors[i].href.replace(/.*#/, '');
                var el = document.getElementById(id);
                if (el !== undefined)
                    YAHOO.util.Dom.addClass(el, css.hideClass);
                YAHOO.util.Dom.replaceClass(YAHOO.otorg.DomCollapse.coanchors[i], css.openClass, css.parentClass);
            }
        }
        // shortcut to CSS object
        var css = YAHOO.otorg.DomCollapse.css;
        hideAll(this);
        // get the trigger element, which is the one clicked on if it has
        // the trigger class
        var parent = YAHOO.util.Dom.hasClass(this, css.triggerClass) ? this : this.parentNode;
        // retrieve the ID from the href attribute and make sure the element 
        // exists
        var id = this.href.replace(/.*#/, '');
        var t = document.getElementById(id);
        if (t !== undefined) {
            // get overflow style - elements need to have overflow hidden 
            // to animate smoothly
            var oldover = YAHOO.util.Dom.getStyle(t, 'overflow');
            // set overflow to hidden
            YAHOO.util.Dom.setStyle(t, 'overflow', 'hidden');
            // get the height and compare it to the offsetHeight, thus 
            // getting the real height + padding
            var height = YAHOO.util.Dom.getStyle(t, 'height');
            if (height === 'auto') {
                curHeight = t.offsetHeight;
            } else {
                curHeight = Math.max(parseInt(height), t.offsetHeight);
            }
            var x = parseInt(curHeight);
            // if the element is currently hidden
            if (YAHOO.util.Dom.hasClass(t, css.hideClass)) {
                // set its height and opacity to 0 and add the height class
                YAHOO.util.Dom.setStyle(t, 'height', 0 + 'px');
                YAHOO.util.Dom.removeClass(t, css.hideClass);
                YAHOO.util.Dom.setStyle(t, 'opacity', 0);
                // prepare the animation animate the element
                var a = new YAHOO.util.Anim(t, {
                    opacity : {
                        from : 0,
                        to : 1
                    },
                    height : {
                        from : 0,
                        to : x
                    }
                }, .8, YAHOO.util.Easing.easeBoth);
                // re-set the overflow and set the appropriate class to the 
                // trigger element when the animation has finished
                a.onComplete.subscribe(function() {
                    YAHOO.util.Dom.setStyle(t, 'overflow', oldover);
                    YAHOO.util.Dom.replaceClass(parent, css.parentClass, css.openClass);
                });
                // animate the element
                a.animate();
                // if the element is currently visible
            } else {
                // prepare and start the animation
                var a = new YAHOO.util.Anim(t, {
                    opacity : {
                        from : 1,
                        to : 0
                    },
                    height : {
                        from : x,
                        to : 0
                    }
                }, .8, YAHOO.util.Easing.easeBoth);
                a.animate();
                // when the animation is done, hide it with the right class, 
                // re-set its styles and replace the open with a parent class
                a.onComplete.subscribe(function() {
                    YAHOO.util.Dom.addClass(t, css.hideClass);
                    YAHOO.util.Dom.setStyle(t, 'height', x + 'px');
                    YAHOO.util.Dom.setStyle(t, 'opacity', 1);
                    YAHOO.util.Dom.setStyle(t, 'overflow', oldover);
                    YAHOO.util.Dom.replaceClass(parent, css.openClass, css.parentClass);
                });
            }
            ;
        }
        ;
        YAHOO.util.Event.preventDefault(e);
    };
};
