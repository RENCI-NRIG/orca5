/*
  YUIDomCollapse by Christian Heilmann
  Version 1.0 / May 2007
  License: http://creativecommons.org/licenses/by/3.0/
  Homepage: http://onlinetools.org/tools/yuidomcollapse/
 */
// YUI Namespace
YAHOO.namespace('otorg');

YAHOO.otorg.DomCollapse = {
    coanchors : [],
    init : function() {
        // shortcut for CSS properties
        var css = YAHOO.otorg.DomCollapse.css;
        if (typeof (css) !== 'undefined') {
            // get possible bookmark
            var bookmark = window.location.hash.replace('#', '');
            // get all elements with the correct class
            var elms = YAHOO.util.Dom.getElementsByClassName(css.triggerClass);
            // loop over all the elements
            for (var i = 0, j = elms.length; i < j; i++) {
                // if the trigger is not a link
                if (elms[i].nodeName.toLowerCase() !== 'a') {
                    // get the next element
                    var t = YAHOO.otorg.DomCollapse.getNext(elms[i]);
                    if (t) {
                        // get the element's ID or create a new one
                        var newID = t.id || YAHOO.util.Dom.generateId();
                        t.setAttribute('id', newID);
                        // create a new target and replace the element's
                        // content with this new element
                        var a = document.createElement('a');
                        // save all new elements
                        YAHOO.otorg.DomCollapse.coanchors[i] = a;
                        a.setAttribute('href', '#' + newID);
                        var c = elms[i].innerHTML;
                        a.innerHTML = elms[i].innerHTML;
                        elms[i].innerHTML = '';
                        elms[i].appendChild(a);
                        // if the ID is not the bookmark add the parent class 
                        // to the trigger element and hide the element by 
                        // adding the hide class
                        if (newID !== bookmark) {
                            YAHOO.util.Dom.addClass(elms[i], css.parentClass);
                            YAHOO.util.Dom.addClass(t, css.hideClass);
                            // otherwise remove the hide class and add the 
                            // open class to the trigger
                        } else {
                            YAHOO.util.Dom.addClass(elms[i], css.openClass);
                            YAHOO.util.Dom.removeClass(t, css.hideClass);
                        }
                        ;
                        // add a click handler to the link pointing to toggle()
                        YAHOO.util.Event.on(a, 'click', YAHOO.otorg.DomCollapse.toggle);
                    }
                    ;
                    // if the trigger is a link
                } else {
                    YAHOO.otorg.DomCollapse.coanchors[i] = elms[i];
                    // get the ID from the href attribute
                    var newID = elms[i].href.replace(/.*#/, '');
                    // grab the connected element, or the next sibling in case
                    // it doesn't exist
                    var t = document.getElementById(newID) || YAHOO.otorg.DomCollapse.getNext(elms[i]);
                    if (t !== null) {
                        // re-set the href attribute to this element
                        if (t.id !== newID) {
                            newID = t.id;
                            elms[i].setAttribute('href', '#' + newID);
                        }
                        ;
                        // if the ID is not the bookmark, hide the element
                        // and add the parent class
                        if (newID !== bookmark) {
                            YAHOO.util.Dom.addClass(elms[i], css.parentClass);
                            YAHOO.util.Dom.addClass(t, css.hideClass);
                            // otherwise add the open class to the trigger
                        } else {
                            YAHOO.util.Dom.addClass(elms[i], css.openClass);
                        }
                        ;
                        // add a click handler to the link pointing to toggle
                        YAHOO.util.Event.on(elms[i], 'click', YAHOO.otorg.DomCollapse.toggle);
                    }
                    ;
                }
                ;
            }
            ;

        }
        ;
    },
    // tool method to get the next sibling that is not a text node
    getNext : function(o) {
        var t = o.nextSibling;
        if (t) {
            while (t.nodeType !== 1 && t.nextSibling) {
                t = t.nextSibling;
            }
        }
        return t;
    },
    // method to toggle the showing and hiding of the next element
    toggle : function(e) {
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
        // shortcut for CSS object
        var css = YAHOO.otorg.DomCollapse.css;
        hideAll(this);
        // if the element has the trigger class it is a link, otherwise 
        // it is a generated link by init()
        var parent = YAHOO.util.Dom.hasClass(this, css.triggerClass) ? this : this.parentNode;
        // grab the ID the link points to from the href attribute and get the element
        var id = this.href.replace(/.*#/, '');
        var t = document.getElementById(id);
        if (t !== undefined) {
            // if the element is hidden (has the hide class) remove the hide 
            // class and swap parent for open and vice versa
            if (YAHOO.util.Dom.hasClass(t, css.hideClass)) {
                YAHOO.util.Dom.removeClass(t, css.hideClass);
                YAHOO.util.Dom.replaceClass(parent, css.parentClass, css.openClass);
            } else {
                YAHOO.util.Dom.addClass(t, css.hideClass);
                YAHOO.util.Dom.replaceClass(parent, css.openClass, css.parentClass);
                // don't follow the link when you hide the element
                YAHOO.util.Event.preventDefault(e);
            }
            ;
        }
        ;
    }
};
YAHOO.util.Event.onDOMReady(YAHOO.otorg.DomCollapse.init);
