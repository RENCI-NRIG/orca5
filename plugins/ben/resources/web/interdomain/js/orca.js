// Semantic ORCA GUI 0.2

// Author: Ilia Baldine (ibaldin@renci.org)
// This code utilizes Yahoo YUI2, Google Maps API and uses code 
// from MIT Tabulator project (http://www.w3.org/2005/ajar/tab)

var kb = new RDFIndexedFormula(); // This uses indexing and smushing
var sf = new SourceFetcher(kb); // This handles resource retrieval
kb.register('dc', "http://purl.org/dc/elements/1.1/");
kb.register('rdf', "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
kb.register('rdfs', "http://www.w3.org/2000/01/rdf-schema#");
kb.register('owl', "http://www.w3.org/2002/07/owl#");
kb.register('Resource', "http://geni-orca.renci.org/owl/compute.owl#");
kb.register('collections', "http://geni-orca.renci.org/owl/collections.owl#");

// we register these as namespaces, but we also use them for labeling
kb.register('BEN', "http://geni-orca.renci.org/owl/ben.rdf#");
kb.register('NLR', "http://geni-orca.renci.org/owl/nlr.rdf#");
kb.register('UMass', "http://geni-orca.renci.org/owl/mass.rdf#");
kb.register('StarLight', "http://geni-orca.renci.org/owl/starlight.rdf#");
kb.register('DukeCSEuca', "http://geni-orca.renci.org/owl/dukevmsite.rdf#");
kb.register('RenciEuca', "http://geni-orca.renci.org/owl/rencivmsite.rdf#");
kb.register('UNCEuca', "http://geni-orca.renci.org/owl/uncvmsite.rdf#");

kb.predicateCallback = AJAR_handleNewTerm;
kb.typeCallback = AJAR_handleNewTerm;

var tooltip;

// buttons
var findButton, startButton, checkButton, cancelButton, submitButton;

var requestEndpoints = {
    providerRowCount : 0,
    maxRowCount : 8,
    minEndpoints : 2,
    providerTable : null,
    providerTableSchema : [ "labelSet", "domain", "pop1", "typeOfResource", "units" ]
};

// all domains (each domain has color (for icons) and popsByName - index of pops by Uri)
// each pop has friendlyName, lat, lon and google maps marker)
var domains = [];

// all resources (each resource has a labelSet, pop1, domain, units, friendlyName, typeOfResource)
var resources = [];

// using google icon colors
var iconColorPicker = {
    colors : [ "green", "red", "blue", "orange", "purple", "yellow" ],
    index : 0
};

// filled in automatically
var mapIcons = [];

// google map
var gMap;

// site models to load
var siteModels = [ "http://geni-orca.renci.org/owl/mass.rdf", "http://geni-orca.renci.org/owl/ben.rdf",
        "http://geni-orca.renci.org/owl/dukevmsite.rdf", "http://geni-orca.renci.org/owl/rencivmsite.rdf",
        "http://geni-orca.renci.org/owl/uncvmsite.rdf",
        //"http://geni-orca.renci.org/owl/nlr.rdf",
        "http://geni-orca.renci.org/owl/starlight.rdf" ];

// schemas to load
var ndlSchemas = [ "http://geni-orca.renci.org/owl/topology.owl", "http://geni-orca.renci.org/owl/layer.owl",
        "http://geni-orca.renci.org/owl/collections.owl", "http://geni-orca.renci.org/owl/location.owl",
        "http://geni-orca.renci.org/owl/compute.owl", "http://geni-orca.renci.org/owl/dtn.owl",
        "http://geni-orca.renci.org/owl/ethernet.owl", "http://geni-orca.renci.org/owl/itu-grid.owl",
        "http://geni-orca.renci.org/owl/orca.owl", "http://geni-orca.renci.org/owl/request.owl",
        "http://geni-orca.renci.org/owl/storage.owl", "http://geni-orca.renci.org/owl/collections.owl" ];

var guiQueries = {
    allDevices : " SELECT ?dev WHERE { ?dev <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/topology.owl#Device> . }",
    deviceLocations : "SELECT ?dev ?lat ?lon \
WHERE \
{ \
    ?dev <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/topology.owl#Device> . \
    ?pop1 <http://geni-orca.renci.org/owl/location.owl#locatedAt> ?loc . \
    ?pop1 <http://geni-orca.renci.org/owl/collections.owl#element> ?dev . \
    ?loc <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . \
    ?loc <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon . \
}",
    domains : "SELECT ?dom \
WHERE \
{ \
    ?dom <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/topology.owl#NetworkDomain> . \
}",
    pops : "SELECT ?pop1 ?lat ?lon \
WHERE \
{ \
    ?pop1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/collections.owl#Set> . \
    ?pop1 <http://geni-orca.renci.org/owl/location.owl#locatedAt> ?loc . \
    ?loc <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . \
    ?loc <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon . \
}",
    popsForDomain : "SELECT ?popUri ?lat ?lon \
WHERE					\
{ \
    ?%%% <http://geni-orca.renci.org/owl/collections.owl#element> ?popUri . \
    ?popUri <http://geni-orca.renci.org/owl/location.owl#locatedAt> ?loc . \
    ?loc <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . \
    ?loc <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon . \
}",
    // Tabulator does not do unions, so we'll need to do substitutions
    // domain, labelset name, size, pop, type
    resourceunits : "SELECT ?labelSet ?domain ?pop1 ?typeOfResource ?units \
WHERE \
{  \
    ?domain <http://geni-orca.renci.org/owl/domain.owl#hasService> ?ser . \
    ?ser ?%%% ?labelSet . \
    ?labelSet <http://geni-orca.renci.org/owl/collections.owl#size> ?units . \
    ?labelSet <http://geni-orca.renci.org/owl/domain.owl#hasResourceType> ?typeOfResource . \
    ?domain <http://geni-orca.renci.org/owl/collections.owl#element> ?pop1 . \
}",
    // find VM-like units and their associated info (device needed for connection requests):
    vmUnits : "SELECT ?labelSet ?domain ?pop1 ?typeOfResource ?units ?device  \
WHERE  \
{  \
    ?device <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/topology.owl#Device> . \
    ?domain <http://geni-orca.renci.org/owl/domain.owl#hasService> ?ser . \
    ?ser <http://geni-orca.renci.org/owl/compute.owl#availableVMLabelSet> ?labelSet . \
    ?device <http://geni-orca.renci.org/owl/compute.owl#availableVMLabelSet> ?labelSet . \
    ?labelSet <http://geni-orca.renci.org/owl/collections.owl#size> ?units . \
    ?labelSet <http://geni-orca.renci.org/owl/domain.owl#hasResourceType> ?typeOfResource . \
    ?domain <http://geni-orca.renci.org/owl/collections.owl#element> ?pop1 . \
}",
    // find port-like units and their associated info (device needed for connection requests):
    portUnits : "SELECT ?labelSet ?domain ?pop1 ?typeOfResource ?units ?device  \
WHERE  \
{  \
    ?domain <http://geni-orca.renci.org/owl/domain.owl#hasService> ?ser . \
    ?labelSet <http://geni-orca.renci.org/owl/collections.owl#size> ?units . \
    ?labelSet <http://geni-orca.renci.org/owl/domain.owl#hasResourceType> ?typeOfResource . \
    ?ser <http://geni-orca.renci.org/owl/compute.owl#availablePortLabelSet> ?labelSet . \
    ?interface <http://geni-orca.renci.org/owl/compute.owl#availablePortLabelSet> ?labelSet . \
    ?device <http://geni-orca.renci.org/owl/topology.owl#hasInterface> ?interface . \
    ?domain <http://geni-orca.renci.org/owl/collections.owl#element> ?pop1 . \
}",
    // helper query: find device from availableVMLabelSet (VM-like resources)
    deviceFromVMLabelSet : "SELECT ?device \
WHERE \
{ \
    ?device <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/topology.owl#Device> .\
    ?device <http://geni-orca.renci.org/owl/compute.owl#availableVMLabelSet> ?%%% . \
}",

    // helper query: find device from availablePortLabelSet (port-like resources)
    deviceFromPortLabelSet : "SELECT ?device \
WHERE \
{ \
    ?interface <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://geni-orca.renci.org/owl/topology.owl#Interface> . \
    ?interface <http://geni-orca.renci.org/owl/compute.owl#availablePortLabelSet> ?%%% . \
    ?device <http://geni-orca.renci.org/owl/topology.owl#hasInterface> ?interface . \
}"
};

// these should be probably tied via the ontology
var knownResourceTypes = {
    VM : {
        unitCount : "orca:numORCAUnitServer",
        ls : "<http://geni-orca.renci.org/owl/compute.owl#availableVMLabelSet>",
        query : guiQueries.vmUnits
    },
    GEPort : {
        unitCount : "ethernet:numGigabitEthernetPort",
        ls : "<http://geni-orca.renci.org/owl/compute.owl#availablePortLabelSet>",
        query : guiQueries.portUnits
    },
    TenGEPort : {
        unitCount : "ethernet:numTenGigabitEthernetPort",
        ls : "<http://geni-orca.renci.org/owl/compute.owl#availablePortLabelSet>",
        query : guiQueries.portUnits
    },
    ViseTestbed : {
        unitCount : "orca:numORCAUnitServer",
        ls : "<http://geni-orca.renci.org/owl/compute.owl#availableVMLabelSet>",
        query : guiQueries.vmUnits
    }
};

/////////////////////
// GUI section (buttons etc)
/////////////////////
function onFindButtonClick(p_oEvent) {
    RunNDLQuery();
}

function onStartButtonClick(p_oEvent) {
    startButton.setAttributes({
        disabled : true
    });
    checkButton.setAttributes({
        disabled : false
    });
    cancelButton.setAttributes({
        disabled : false
    });
    InitializeView();
}

function onCheckButtonClick(p_oEvent) {
    // print out RDF request in a new window

    var win = window.open('javascript:void(0);', 'rdfWindow');
    win.document.write(escapeForXML(formRDFRequest()));
    //CheckState();
}

function onMapButtonClick(p_oEvent) {
    UpdateMap();
}

function onCancelButtonClick(p_oEvent) {
    var i;
    if (requestEndpoints.providerTable !== null) {
        requestEndpoints.providerTable.destroy();
        requestEndpoints.providerTable = null;
    }
    requestEndpoints.providerRowCount = 0;
    submitButton.setAttributes({
        disabled : true
    });
}

/* submit the request  */
function onSubmitButtonClick(p_oEvent) {

    // insert into the form
    document.myform.ndl.value = formRDFRequest();
    document.myform.submit();
}

/* submit the request  */
function onSubmitTAButtonClick(p_oEvent) {

    // insert into the form
    document.myform.ndl.value = document.myform.ndldirect.value;
    document.myform.submit();
}

/////////////////////////
// RDF generation section
/////////////////////////

function addLeadingZero(t) {

    if (t.length == 1)
        return "0" + t;
    else
        return t;
}

// takes time as js object and returns
// a string compliant with XML Time type
function convertToXMLTime(d, t) {
    var ret = ""

    ret += d.getFullYear() + "-" + addLeadingZero(String(d.getMonth() + 1)) + "-" + addLeadingZero(String(d.getDate()));
    ret += "T" + String(t) + ":00Z";

    return ret;
}

// describe resources needed from the domain
function domainUnitRequest(rec) {

    rdfRequest = "";

    rdfRequest += '<rdf:Description rdf:about="' + uriStripBrackets(rec.getData('device')) + '">';
    if (knownResourceTypes[rec.getData('typeOfResource')] === undefined)
        tlog('Resource type ' + rec.getData('typeOfResource') + ' is not known');

    rdfRequest += '<' + knownResourceTypes[rec.getData('typeOfResource')].unitCount + ' rdf:datatype="&xsd;integer">'
            + rec.getData('selected') + '</' + knownResourceTypes[rec.getData('typeOfResource')].unitCount + '>';
    rdfRequest += '<request:inDomain rdf:resource="' + uriStripBrackets(rec.getData('nsdomain')) + '"/>';
    rdfRequest += '</rdf:Description>';

    return rdfRequest;
}

function formRDFRequest() {

    var i, j;

    tlog("Forming request\n");

    var rdfRequest = "";
    // these will be the connections
    var connections = [];

    // get the table recordset
    var rs = requestEndpoints.providerTable.getRecordSet();
    // rs.getRecord(index).getData("domain") and rs.getRecord(index).getData("units") etc
    // gets us what we want

    // somehow we determine how many connections we need, for now it is one
    connections[0] = new Array();
    connections[0]['name'] = "#Req/conn/0";
    connections[0]['bandwidth'] = document.myform.bandwidth.value;
    var toFromFlag = 0;

    for (i = 0; i < rs.getLength(); i++) {
        if (rs.getRecord(i).getData("selected") != "0")
            if (toFromFlag == 0) {
                connections[0]['from'] = rs.getRecord(i);
                toFromFlag = 1;
            } else
                connections[0]['to'] = rs.getRecord(i);
    }
    tlog("Connection from " + connections[0]['from'].getData("domain") + " to "
            + connections[0]['to'].getData("domain") + "\n");

    // add Reservation
    //     <request:Reservation rdf:about="#Ben-Mass-Reservation2">
    //         <request:startingTime
    //             >2009-07-07T13:00:00Z</request:startingTime>
    //         <request:endingTime
    //             >2009-07-08T13:00:00Z</request:endingTime>
    //         <layer:atLayer rdf:resource="&ethernet;EthernetNetworkElement"/>
    //         <collections:element rdf:resource="#UNC-Mass-Request4/conn/1"/>
    //     </request:Reservation>

    var resId = "#Reservation" + String(Math.round(Math.random() * 1000));
    var startDate = new Date(document.myform.start.value.split(' ')[0]);
    var startTime = document.myform.start.value.split(' ')[1];
    var endDate = new Date(document.myform.end.value.split(' ')[0]);
    var endTime = document.myform.end.value.split(' ')[1];

    rdfRequest += '<request:Reservation rdf:about="' + resId + '"><request:startingTime>'
            + convertToXMLTime(startDate, startTime) + '</request:startingTime>';
    rdfRequest += '<request:endingTime>' + convertToXMLTime(endDate, endTime) + '</request:endingTime>';
    for (i = 0; i < connections.length; i++) {
        rdfRequest += '<collections:element rdf:resource="' + connections[i]['name'] + '"/>';
        rdfRequest += '<layer:atLayer rdf:resource="&ethernet;EthernetNetworkElement"/>';
    }
    rdfRequest += '</request:Reservation>';

    // add connections and resource unit counts for each domain
    //    <topology:NetworkConnection rdf:about="#UNC-Mass-Request4/conn/1">
    //        <layer:bandwidth rdf:datatype="&xsd;long">100000000</layer:bandwidth>
    //        <topology:hasInterface rdf:resource="&UMass;Vise/Testbed"/>
    //        <topology:hasInterface rdf:resource="&UNC;Euca"/>
    //    </topology:NetworkConnection>

    for (i = 0; i < connections.length; i++) {
        rdfRequest += '<topology:NetworkConnection rdf:about="' + connections[i]['name'] + '">';
        rdfRequest += '<layer:bandwidth rdf:datatype="&xsd;long">' + connections[i]['bandwidth'] + '</layer:bandwidth>';
        rdfRequest += '<topology:hasInterface rdf:resource="'
                + uriStripBrackets(connections[i]['from'].getData('device')) + '"/>';
        rdfRequest += '<topology:hasInterface rdf:resource="'
                + uriStripBrackets(connections[i]['to'].getData('device')) + '"/>';
        rdfRequest += '</topology:NetworkConnection>';
        // add resource counts in domains
        // currently support numORCAUnitServer, numGigabitEthernetPort, numTenGigabitEthernetPort
        // see knownResourceTypes
        rdfRequest += '<!-- Request from domain ' + connections[i]['from'].getData('domain') + ' -->';
        rdfRequest += domainUnitRequest(connections[i]['from']);
        rdfRequest += '<!-- Request from domain ' + connections[i]['to'].getData('domain') + ' -->';
        rdfRequest += domainUnitRequest(connections[i]['to']);
    }

    return RDFHeader + rdfRequest + RDFFooter;
}

////////////////////
// view (Map and tables) generation
////////////////////

// main function rendering the layout and map controls
function myload() {

    var Event = YAHOO.util.Event;

    Event.onDOMReady(function() {

        //      // create a panel
        //       var guiPanel = new YAHOO.widget.Panel('gui-panel', {
        //         draggable: false,
        // 	    close: false,
        // 	    autofillheight: "body", // default value, specified here to highlight its use in the example            
        // 	    width: '800px',
        // 	    height: '500px',        
        // 	    xy: [100, 100]
        // 	    });
        //       guiPanel.setHeader('SUPER-DUPER ORCA GUI');

        //       guiPanel.beforeRenderEvent.subscribe(function() {

        Event.onAvailable('layout', function() {
            var layout = new YAHOO.widget.Layout('layout', {
                units : [ {
                    position : 'left',
                    width : 350,
                    header : 'Resource Selection',
                    resize : true,
                    body : 'left1',
                    gutter : '5px',
                    collapse : true,
                    collapseSize : 50,
                    scroll : true,
                    animate : true
                }, {
                    position : 'center',
                    width : 500,
                    body : 'center1',
                    header : 'Resource Map',
                    gutter : '5px'
                }, {
                    position : 'bottom',
                    height : 40,
                    resize : true,
                    body : 'bottom1',
                    gutter : '5px',
                    collapse : false
                } ]
            });
            layout.render();
        });
    });
    //      guiPanel.render();
    //     });

    // create log reader in collapsed state
    //var el = document.getElementById("logger");
    //var myContainer = document.body.appendChild(document.createElement("div"));
    //var myLogReader = new YAHOO.widget.LogReader(myContainer);
    // var myLogReader = new YAHOO.widget.LogReader(myContainer, {draggable: true, verboseOutput:false, width:'780px'});
    //myLogReader.setTitle("System log");
    //myLogReader.collapse();

    // BUTTONs
    //findButton = new YAHOO.widget.Button("findbutton"); 
    //findButton.on("click", onFindButtonClick);

    startButton = new YAHOO.widget.Button("startbutton");
    startButton.on("click", onStartButtonClick);

    checkButton = new YAHOO.widget.Button("checkbutton", {
        disabled : true
    });
    checkButton.on("click", onCheckButtonClick);

    cancelButton = new YAHOO.widget.Button("cancelbutton", {
        disabled : true
    });
    cancelButton.on("click", onCancelButtonClick);

    submitButton = new YAHOO.widget.Button("submitbutton", {
        disabled : true
    });
    submitButton.on("click", onSubmitButtonClick);

    submitTAButton = new YAHOO.widget.Button("submitTAbutton");
    submitTAButton.on("click", onSubmitTAButtonClick);

    //var mapButton = new YAHOO.widget.Button("mapbutton");
    //mapButton.on("click", onMapButtonClick);

    tinfo("Loading schemas");
    for (i in ndlSchemas) {
        sf.request(kb.sym(ndlSchemas[i]));
    }

    tinfo("Loading site models");
    for (i in siteModels) {
        sf.request(kb.sym(siteModels[i]));
    }

    loadMap();

    // tooltips
    tooltip = document.createElement("div");
    document.getElementById("map_canvas").appendChild(tooltip);
    tooltip.style.visibility = "hidden";
}

///////////////////////
// RDF query section
///////////////////////

/** returns true if str starts with pref, case sensitive, space sensitive **/
function string_startswith(str, pref) { // missing library routines
    return (str.slice(0, pref.length) == pref);
}

/** callback handler for new terms **/
function AJAR_handleNewTerm(kb, p, requestedBy) {
    //tdebug("entering AJAR_handleNewTerm w/ kb, p=" + p + ", requestedBy=" + requestedBy);
    if (p.termType != 'symbol')
        return;
    var docuri = p;
    var fixuri;
    if (p.uri.indexOf('#') < 0) {

        if (string_startswith(p.uri, 'http://xmlns.com/foaf/0.1/')) {
            fixuri = "http://dig.csail.mit.edu/2005/ajar/ajaw/test/foaf";
            // should give HTTP 303 to ontology -- now is :-)
        } else

        if (string_startswith(p.uri, 'http://purl.org/dc/elements/1.1/')
                || string_startswith(p.uri, 'http://purl.org/dc/terms/')) {
            fixuri = "http://dublincore.org/2005/06/13/dcq";
            //dc fetched multiple times
        } else if (string_startswith(p.uri, 'http://xmlns.com/wot/0.1/')) {
            fixuri = "http://xmlns.com/wot/0.1/index.rdf";
        } else if (string_startswith(p.uri, 'http://web.resource.org/cc/')) {
            //            twarn("creative commons links to html instead of rdf. doesn't seem to content-negotiate.");
            fixuri = "http://web.resource.org/cc/schema.rdf";
        }
    }
    if (fixuri) {
        docuri = kb.sym(fixuri)
    }
    if (sf.getState(docuri) != 'unrequested')
        return;

    if (fixuri) { // only give warning once: else happens too often
        twarn("Assuming server still broken, faking redirect of <" + p.uri + "> to <" + docuri.uri + ">")
    }

    sf.request(docuri, requestedBy);
} //AJAR_handleNewTerm

// testing/helper function
function RunNDLQuery() {

    query = guiQueries.testbedunits;
    tinfo("Starting query " + query);

    q = SPARQLToQuery(query);

    function drawer(bindings) {
        var i, tr, td;
        tr = document.createElement('tr');
        //t.appendChild(tr);
        var newRow = {};
        for (i = 0; i < nv; i++) {
            v = q.vars[i];
            newRow[v.label] = escapeForXML(String(bindings[v]));
            //tr.appendChild(bindings[v]);
        } //for each query var, make a row
        dataTable.addRow(newRow, 0);
    }

    var i, nv = q.vars.length, j, v;

    var columnHeads = [];
    var schema = [];
    for (i = 0; i < nv; i++) {
        v = q.vars[i];
        columnHeads.push({
            key : v.label,
            sortable : true
        });
        schema.push(v.label);
    }

    var dataSource = new YAHOO.util.DataSource([]);
    dataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
    dataSource.responseSchema = {
        fields : schema
    };

    var dataTable = new YAHOO.widget.DataTable("result1", columnHeads, dataSource, {});

    // execute the query
    kb.query(q, drawer, myFetcher);

    tinfo("Finished");
}

function myFetcher(x, requestedBy) {
    if (x == null) {
        fyi("@@ SHOULD SYNC NOW"); // what does this mean?
    } else {
        fyi("Fetcher: " + x);
        AJAR_handleNewTerm(kb, x, requestedBy);
    }
}

function uriToNamespace(uri) {
    // get the namespace of a Thing from its URI
    var first = uri.split('#')[0];
    return first.substring(1, first.length) + "#";
}

function uriToFriendlyString(uri) {

    var sp = uri.split('#');
    var ns = sp[0].substring(1, sp[0].length) + "#";

    // return short name
    return kb.reverseNamespaces[ns] + " " + sp[1].substring(0, sp[1].length - 1);
}

function uriToFriendlyNS(uri) {

    var sp = uri.split('#');
    var ns = sp[0].substring(1, sp[0].length) + "#";

    // return short name
    return kb.reverseNamespaces[ns];
}

function uriToNoNS(uri) {
    var sp = uri.split('#');
    var ns = sp[0].substring(1, sp[0].length) + "#";

    // return short name
    return sp[1].substring(0, sp[1].length - 1);
}

function uriStripBrackets(uri) {
    return uri.substring(1, uri.length - 1);
}

// show reservable resources at a PoP
function showPopResourcesForm(domain, pop) {
    tinfo("Showing resources for pop " + pop + " domain " + domain);
    var provider;

    function displayProviderUnits() {
        var newRow = new Array();

        // populate the data source table
        newRow['labelSet'] = resources[provider]['friendlyName'];
        newRow['typeOfResource'] = uriToNoNS(resources[provider]['typeOfResource']);
        newRow['units'] = resources[provider]['units'];
        newRow['selected'] = "0";
        newRow['pop1'] = resources[provider]['pop1'];
        newRow['domain'] = uriToNoNS(resources[provider]['domain']);
        newRow['nsdomain'] = resources[provider]['domain'];
        newRow['device'] = resources[provider]['device'];

        dataTable.addRow(newRow, 0);
    }

    var dataTable;
    function createProviderTable() {
        // create a table
        dataTable = new YAHOO.widget.DataTable("result0", columnHeads, dataSource, {});
        requestEndpoints.providerTable = dataTable;

        var highlightEditableCell = function(oArgs) {
            var elCell = oArgs.target;
            if (YAHOO.util.Dom.hasClass(elCell, "yui-dt-editable")) {
                this.highlightCell(elCell);
            }
        };
        dataTable.subscribe("cellMouseoverEvent", highlightEditableCell);
        dataTable.subscribe("cellMouseoutEvent", dataTable.onEventUnhighlightCell);
        dataTable.subscribe("cellClickEvent", dataTable.onEventShowCellEditor);
    }

    // create table header
    var columnHeads = [
    // { key: "provider", label: "<b>Resource Provider</b>"},
    {
        key : "domain",
        label : "<b>Domain</b>"
    }, {
        key : "typeOfResource",
        label : "<b>Resource Type</b>"
    }, {
        key : "units",
        label : "<b>Available Units</b>"
    }, {
        key : "selected",
        label : "<b>Selected Units</b>",
        editor : new YAHOO.widget.TextboxCellEditor({
            disableBtns : true
        })
    } ];
    var dataSource = new YAHOO.util.DataSource([]);
    dataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
    dataSource.responseSchema = {
        fields : requestEndpoints.providerTableSchema
    };

    for (provider in resources) {
        tinfo("Labelset " + resources[provider]['friendlyName'] + " at pop " + resources[provider]['pop1']);
        // if provider matches pop and domain, add to table
        if ((resources[provider]['pop1'] == pop) && (resources[provider]['domain'] == domain)) {
            // add row to a table
            if (requestEndpoints.providerTable == null) {
                createProviderTable();
            }
            dataTable = requestEndpoints.providerTable;

            displayProviderUnits();

            if (requestEndpoints.providerRowCount == requestEndpoints.minEndpoints - 1)
                submitButton.setAttributes({
                    disabled : false
                });
            requestEndpoints.providerRowCount = (++requestEndpoints.providerRowCount) % requestEndpoints.maxRowCount;
        }
    }
}

// get PoPs for specific NetworkDomain and display them on the map
function getPopsForDomain(domain) {
    tinfo("    Getting the pops for domain " + domain);

    // substitute for query
    var query = guiQueries.popsForDomain.replace("?%%%", domain);
    var q = SPARQLToQuery(query);
    var nv = q.vars.length;

    function processResults(bindings) {
        var i;

        var v = q.vars[0];
        var popUri = String(bindings[v]);
        domains[domain].popsByName[popUri] = new Array();
        domains[domain].popsByName[popUri]['friendlyName'] = uriToFriendlyString(popUri);

        for (i = 1; i < nv; i++) {
            v = q.vars[i];
            domains[domain].popsByName[popUri][v.label] = String(bindings[v]);
        }
        tinfo("Added pop " + domains[domain].popsByName[popUri]['friendlyName'] + " to domain " + domain);

        // add PoP to the map with slightly randomized coordinates
        var point = new GLatLng(parseFloat(domains[domain].popsByName[popUri]['lat']) + Math.random() / 100.0,
                parseFloat(domains[domain].popsByName[popUri]['lon']) + Math.random() / 100.0);
        domains[domain].popsByName[popUri]['marker'] = createMarker(point,
                domains[domain].popsByName[popUri]['friendlyName'], domain, popUri, mapIcons[domains[domain].color]);
        gMap.addOverlay(domains[domain].popsByName[popUri]['marker']);
    }

    // execute the query
    kb.query(q, processResults, myFetcher);
}

// retrieve a list of domains and assign each a random color
function getDomains() {

    tinfo("Getting a list of domains");
    var q = SPARQLToQuery(guiQueries.domains);
    var nv = q.vars.length;

    function processResults(bindings) {
        var v = q.vars[0];
        var mydom = String(bindings[v]);
        domains[mydom] = {
            color : iconColorPicker.colors[iconColorPicker.index],
            popsByName : []
        };
        iconColorPicker.index = (++iconColorPicker.index) % iconColorPicker.colors.length;
        tinfo("     Adding domain " + mydom + " " + domains[mydom].color);
        getPopsForDomain(mydom);
    }

    // execute the query 
    kb.query(q, processResults, myFetcher);

}

// get different types of resource providers and units of whatever it is they have
function getResources(query, subst) {

    tinfo("Getting a list of resources");
    var newQuery = query.replace("?%%%", subst);

    var q = SPARQLToQuery(newQuery);
    var nv = q.vars.length;

    function processResults(bindings) {
        var i;
        var v = q.vars[0];
        var ls = String(bindings[v]); // labelset
        resources[ls] = new Array();
        resources[ls]['friendlyName'] = uriToFriendlyString(ls);

        // parse the rest of the result ('units', 'domain', 'pop1')
        for (i = 1; i < nv; i++) {
            v = q.vars[i];
            resources[ls][v.label] = String(bindings[v]);
        }

        tinfo("   ADDING RESOURCE " + resources[ls]['friendlyName'] + " at pop " + escapeForXML(resources[ls]['pop1']));
    }

    // execute the query 
    kb.query(q, processResults, myFetcher);
}

function InitializeView() {

    // create initial data structures for displaying
    getDomains(); // also gets pops in domains

    // get all resource providers and units of resources available
    for (resType in knownResourceTypes) {
        getResources(knownResourceTypes[resType].query, "");
    }
}

function CheckState() {
    for (domain in domains) {
        tinfo("Domain " + domain);
        for (pop in domains[domain].popsByName)
            tinfo("  POP: " + pop + " short name " + domains[domain].popsByName[pop]['friendlyName']);
    }

    for (provider in resources) {
        tinfo("Provider " + provider + " short name: " + resources[provider]['friendlyName'] + " domain: "
                + resources[provider]['domain'] + " resource type " + resources[provider]['typeOfResource']);
    }
}

/////////////////////////////
// GOOGLE MAPS 
/////////////////////////////

// function taken from http://econym.org.uk/gmap/example_maptips2.htm
function showTooltip(marker) {
    tooltip.innerHTML = marker.tooltip;
    var point = gMap.getCurrentMapType().getProjection().fromLatLngToPixel(gMap.getBounds().getSouthWest(),
            gMap.getZoom());
    var offset = gMap.getCurrentMapType().getProjection().fromLatLngToPixel(marker.getPoint(), gMap.getZoom());
    var anchor = marker.getIcon().iconAnchor;
    var width = marker.getIcon().iconSize.width;
    var pos = new GControlPosition(G_ANCHOR_BOTTOM_LEFT, new GSize(offset.x - point.x - anchor.x + width, -offset.y
            + point.y + anchor.y));
    pos.apply(tooltip);
    tooltip.style.visibility = "visible";
}

function createMarker(point, comment, domain, pop, popIcon) {
    var marker = new GMarker(point, {
        icon : popIcon
    }), d = domain, p = pop;
    marker.tooltip = '<div class="tooltip">' + comment + '<\/div>';
    //   GEvent.addListener(marker, "mouseover", function() {
    //       gMap.openInfoWindowHtml(point, comment, {maxWidth: 250});
    //       gMap.updateInfoWindow(null);
    //     });
    GEvent.addListener(marker, "click", function() {
        showPopResourcesForm(domain, pop);
    });
    GEvent.addListener(marker, "mouseover", function() {
        showTooltip(marker);
    });
    GEvent.addListener(marker, "mouseout", function() {
        tooltip.style.visibility = "hidden"
    });

    return marker;
}

function loadMap() {

    // create color icons
    for (color in iconColorPicker.colors) {
        var colorName = iconColorPicker.colors[color];
        mapIcons[colorName] = new GIcon(G_DEFAULT_ICON);
        mapIcons[colorName].iconSize = new GSize(32, 32);
        mapIcons[colorName].image = "http://www.google.com/intl/en_us/mapfiles/ms/micons/" + colorName + "-dot.png";
    }

    if (GBrowserIsCompatible()) {
        var map = new GMap2(document.getElementById("map_canvas"));
        map.setCenter(new GLatLng(34.1, -78.22), 4);
        map.enableScrollWheelZoom();

        gMap = map;

        map.addControl(new GSmallMapControl());
        //map.addControl(new GMapTypeControl());    
    }
}

// draw markers of appropriate colors for PoPs in domains
function UpdateMap() {
    // add markers

    for (domain in domains) {
        for (pop in domains[domain].popsByName) {
            // we randomize points because some PoPs mape to the same location
            var point = new GLatLng(parseFloat(domains[domain].popsByName[pop]['lat']) + Math.random() / 100.0,
                    parseFloat(domains[domain].popsByName[pop]['lon']) + Math.random() / 100.0);
            domains[domain].popsByName[pop]['marker'] = createMarker(point,
                    escapeForXML(domains[domain].popsByName[pop]['popName']), domain,
                    domains[domain].popsByName[pop]['popUri'], mapIcons[domains[domain].color]);
            gMap.addOverlay(domains[domain].popsByName[pop]['marker']);
        }
    }
}
