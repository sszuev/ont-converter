# Ontology Converter v1.0

## A simple command-line utility to convert any rdf graph to OWL2-DL ontology.

###usage: `java -jar ont-converter.jar [-f] [-h] -i <path> [-if <format>] -o      <path> -of <format> [-r] [-s] [-v] [-w]`

###options:

 * -f,--force                     `Ignore exceptions while loading/saving and keep processing ontologies with missed imports`
 * -h,--help                      `Print usage.`
 * -i,--input <path>              `Ontology file or directory with files to read. See --input-format for list of supported output syntaxes. - Required.`
 * -if,--input-format <format>    `The input format. If not specified the program will choose the most suitable one to load ontology from file. Must be one of the following: TURTLE, RDF_XML, RDF_JSON, JSON_LD, NTRIPLES, NQUADS, TRIG, TRIX, RDF_THRIFT, OWL_XML, MANCHESTER_SYNTAX, FUNCTIONAL_SYNTAX, BINARY_RDF, RDFA, OBO, KRSS - Optional.`
 * -o,--output <path>             `Ontology file or directory containing ontologies to write. If the --input is a file then this option parameter must also be a file. - Required.`
 * -of,--output-format <format>   `The format of output ontology/ontologies. Must be one of the following: TURTLE, RDF_XML, RDF_JSON, JSON_LD, NTRIPLES, NQUADS, TRIG, TRIX, RDF_THRIFT, OWL_XML, MANCHESTER_SYNTAX, FUNCTIONAL_SYNTAX, OBO, KRSS2, DL, DL_HTML, LATEX - Required.`
 * -r,--refine                    `Refine output: the resulting ontologies will consist only of the OWL2-DL components.`
 * -s,--spin                      `Use spin transformation to replace rdf:List based spin-constructs (e.g sp:Select) with their text-literal representation to produce compact axioms list`
 * -v,--verbose                   `To print progress info to console.`
 * -w,--web                       `Allow web/ftp diving to retrieve dependent ontologies from owl:imports, otherwise the only specified files will be used as the source.`

                                
###formats aliases (case insensitive):
 * TURTLE              	`0|turtle|ttl`
 * RDF_XML             	`1|rdf_xml|rdf/xml|rdf`
 * RDF_JSON            	`2|rdf_json|rdf/json|rj`
 * JSON_LD             	`3|json_ld|json-ld|jsonld`
 * NTRIPLES            	`4|ntriples|n-triples|nt`
 * NQUADS              	`5|nquads|n-quads|nq`
 * TRIG                	`6|trig`
 * TRIX                	`7|trix`
 * RDF_THRIFT          	`8|rdf_thrift|rdf-thrift|trdf`
 * CSV                 	`9|csv`
 * OWL_XML             	`11|owl_xml|owl/xml|owl`
 * MANCHESTER_SYNTAX   	`12|manchester_syntax|manchestersyntax|omn`
 * FUNCTIONAL_SYNTAX   	`13|functional_syntax|functionalsyntax|fss`
 * BINARY_RDF          	`14|binary_rdf|binaryrdf|brf`
 * RDFA                	`15|rdfa|xhtml`
 * OBO                 	`16|obo`
 * KRSS                	`17|krss`
 * KRSS2               	`18|krss2`
 * DL                  	`19|dl`
 * DL_HTML             	`20|dl_html|dl/html|html`
 * LATEX               	`21|latex|tex`
 
