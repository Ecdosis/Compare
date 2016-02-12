# Compare

Compare is a Java webapp that can be deployed in an application 
container like Tomcat, or it can be debugged in embedded form using 
Jetty. It supports the following services:

1. GET /compare: Parameters are:
    * docid: the document identifer
    * version1: the version to return, with added HTML markup for diffs
    * version2: the version to compare it with
    * diffKind: the name for the differences with version2
2. GET /compare/list: List the versions of an MVD as a HTML select 
element. Parameters are:
    * style: defaults to list/default
    * version1: the version to be initially selected in the dropdown
    * docid: the document to make the list of
    * name: the name of the select element
3. GET /compare/version1: Get the full vid path of the default version. 
Parameters are:
    * docid: the document to get the version1 of
4. GET /compare/version2: get the next version after version1. 
Parameters are:
    * docid: the document in question
    * version1: the first version whose next version is sought
5. GET /compare/table/json: Get a table representation of the MVD in 
JSON. Parameters are:
    * docid: the document to make a table of
    * version1: the base version (default the MVD's version1)
    * offset: the start offset into the table (default 0)
    * length: the length in base to retrieve a table of (default Integer.MAX_VALUE)
    * selected: a comma-separated list of full vids to be selected (default "all")
6. GET /compare/table/html: Get a table representation of the MVD in 
JSON. This was experimental and is now superseded by 
/compare/table/json. Parameters are:
    * docid: the document to make a table of
    * offset: the start offset into the table (default 0)
    * length: the length in base to retrieve a table of (default Integer.MAX_VALUE)
    * SELECTED_VERSIONS: a comma-separated list of full vids to be selected (default "all")
    * SOME_VERSIONS: a boolean: true means selet only some versions
    * WHOLE_WORDS: extend merged sections to whole words
    * COMPACT: compact the table recursively where possible
    * FIRSTID: first merge id for table alignment
