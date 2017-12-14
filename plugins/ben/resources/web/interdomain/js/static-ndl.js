var RDFHeader = '\
<?xml version="1.0"?> \
<!DOCTYPE rdf:RDF [ \
    <!ENTITY owl "http://www.w3.org/2002/07/owl#" > \
    <!ENTITY xsd "http://www.w3.org/2001/XMLSchema#" > \
    <!ENTITY owl2xml "http://www.w3.org/2006/12/owl2-xml#" > \
    <!ENTITY rdfs "http://www.w3.org/2000/01/rdf-schema#" > \
    <!ENTITY orca "http://geni-orca.renci.org/owl/orca.owl#" > \
    <!ENTITY layer "http://geni-orca.renci.org/owl/layer.owl#" > \
    <!ENTITY rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#" > \
    <!ENTITY request "http://geni-orca.renci.org/owl/request.owl#" > \
    <!ENTITY ethernet "http://geni-orca.renci.org/owl/ethernet.owl#" > \
    <!ENTITY topology "http://geni-orca.renci.org/owl/topology.owl#" > \
    <!ENTITY collections "http://geni-orca.renci.org/owl/collections.owl#" > \
]> \
 \
 \
<rdf:RDF xmlns="http://geni-orca.renci.org/owl/idRequest4.rdf#" \
     xml:base="http://geni-orca.renci.org/owl/idRequest4.rdf" \
     xmlns:layer="http://geni-orca.renci.org/owl/layer.owl#" \
     xmlns:orca="http://geni-orca.renci.org/owl/orca.owl#" \
     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" \
     xmlns:owl2xml="http://www.w3.org/2006/12/owl2-xml#" \
     xmlns:ethernet="http://geni-orca.renci.org/owl/ethernet.owl#" \
     xmlns:request="http://geni-orca.renci.org/owl/request.owl#" \
     xmlns:xsd="http://www.w3.org/2001/XMLSchema#" \
     xmlns:owl="http://www.w3.org/2002/07/owl#" \
     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" \
     xmlns:topology="http://geni-orca.renci.org/owl/topology.owl#" \
     xmlns:collections="http://geni-orca.renci.org/owl/collections.owl#"> \
    <owl:Ontology rdf:about=""/>';

var RDFFooter = '</rdf:RDF>';
