# dragonfiles

A Clojure scriptable file processor.
You can think of it as "`awk` on steroids". 

```
                                                                                
                                                                                
   ...........                                                     ..........   
  0MWolcclllllc;;,,'.                  .'.                  .,,,;:cllllcccl0MW; 
   ;OO;             ........          xMMW,          .......             .oKo.  
     .oKx,                  .....     .x0l      .....                 .c0O;     
        :0Xo.                         locx.                         ;ONd.       
          .:c:;;,,''''...........     k: O,    ............''',,,;:cc;          
                 ':oc;;,,''........   ;WKX    ........'',,;:ol,.                
             .OXd;.           .....    0M:    ....           .,c0Kc             
              .cddl,. ..,;,,'..        .:         .'',,;'.  ':odo,              
                  'ldl:'.             'Ocx              .;cod;.                 
                                      ;x.O                                      
                                      ,O,k                                      
                                      '0cd                                      
                                      .Nkl                                      
                                       WM:          -= dragonfiles =-                            
                                       XM;                                  
                                       0M'                                      
                                       kM.                                      
                                       dX                                       
                                       ck                                       
                                       'c                                       
                                                                                
                                                                                

```

## Description

`dragongiles` is a scriptable file processor where scripts can be written
in Clojure. It is somewhat similar to `awk` although it is not an attempt
to clone or copy all the `awk` features.
The idea is that when processing files there is a number of activities
which are more concerning the management of the input/output
rather than the actual processing. So this tool is attempting to
take care of these 'housekeeping' activities while you can concentrate
to write the logic of the actual processing is a modern data oriented
language such as Clojure.

If I had to express with one line what this tools does I would say:

```bash
dgf -s /input/directory -o /output/directory 's/upper-case'
```

The above script takes all the files contained in `/input/directory`, it
applies the function `s/upper-case` to a the lines of the file and
stores the output into `/output/directory`.  The script string contains
an arbitrary Clojure's expressions which must evaluate to a function
which will be applied to each line.  The script can be arbitrary complex
and you can use everything is available in Clojure.

```bash
dgf -s /input/directory -o /output/directory '(comp (partial s/join " ")  #(s/split % #"\W+") s/upper-case)'
```

## Usage

```
SYNOPSIS

       dgf -s PATH -o PATH   SCRIPT   

  -s, --source PATH         A file or directory containing text file to process
  -o, --output PATH         A file or directory which will contains the output files.
  -x, --extension EXT       When processing multiple files use this option to change the output file extension.
  -f, --file-mode           Rather then processing line-by-line the function expects a file-in file-out
  -p, --parallel            Process files in parallel
  -i, --init-script SCRIPT  a function which is executed before the first file is processed
  -e, --end-script SCRIPT   a function which is executed after the last file is processed, and before the termination.
  -q, --quiet               Less verbose output
  -v, --version             Just print the version
  -h, --help                This help
```


  * `-s, --source PATH` (**REQUIRED**) 
  
    This can be either a file or a directory and it represents the
    file(s) to process.  The source path or file must exists.
    
  * `-o, --output PATH` (**REQUIRED**)
  
    This can be either a file or a path and it represents the where the
    processed files must be written. The specified file or directory
    MUST NOT exists.  If the source path (`-s`) points to a file, this
    value will be considered to be the output filename. While if the
    source path is a directory, this value will be considered to be a
    folder. Files written in this directory will have the same directory
    structure of the files in the source directory.
    
  * `-x, --extension`
  
    If present and the source path is pointing to a directory to process
    multiple files it will replace the output file name extension with
    the specified `EXT`. For example if your source is called
    `/tmp/source` which contains files like `myfile.json` and your
    output is `/tmp/output` and you specify `-x tsv`, your output file
    will be called `/tmp/output/myfile.tsv`
    
  * `-f, --file-mode`
  
    The default processing mode is by line, which mean that the `SCRIPT`
    must be a function [`(fn [line] ,,,)`] which takes only one
    parameter which is the line and it must return a string containing
    the output line. When specifying `-f` instead of processing line by
    line the script must be able to accept two parameters, respectively
    the input file and the output file [`(fn [input output] ,,,)`] it is
    then the user responsibility to make sure that the input file is
    parsed properly and written back to the output file in the correct
    format.
    
  * `-p, --parallel`
  
    When this option is specified *dragonfiles* will process each file
    in a separate using a thread-pool. If the script uses a global
    state, it is the user responsibility to ensure the processing is
    thread-safe.  In Clojure this can be easily achieved by using global
    `atoms`.
    
  * `-i, --init-script SCRIPT`

    `SCRIPT` is a Clojure's expression which is evaluated before the first
    file is processed. It can be used to `require` additional namespaces,
    define functions or global var.
    
  * `-e, --end-script SCRIPT`

    `SCRIPT` is a Clojure's expression which is evaluated after the last
    file has been processed and before the termination. This option is
    often used in conjunction with `--init-script` but not necessarily.
    It can be used to "summarise" the total of the processing and print
    it out.
    
  * `-q, --quiet`
  
    When this option is specified the information output is reduced
    to only errors.


  * `SCRIPT` (**REQUIRED**)
  
    This must be a valid Clojure expression which must evaluate to a
    function. If the processing is in `line-mode` (default) then the
    function must take one `String` parameter that is the line of the
    file to process and return another `String` which represents the
    transformed line. You can use any of the the core functions, and
    functions in other namespaces which are shipped with Clojure default
    distribution.
    
    When working on `file-mode` (`-f`) then `SCRIPT` must evaluate to a
    function which accept two parameters: `input-file` and
    `output-file`.  and be responsible for writing the output file in
    the appropriate format.
    
    
## Examples

I will try to list a few examples which will give a glimpse of
*dragonfiles* power.

  * just copy
  
  `dgf -s '/tmp/input' -o '/tmp/output' 'identity'`
  
  This script will run the `identity` function to every line of every
  file present in the directory `/tmp/input` and write it out in a file
  with the same name under `/tmp/output`
  
  
  * "Upper-case all the things"
  
  `dgf -s '/tmp/input' -o '/tmp/output' 's/upper-case'`
  
  The content of every file in input will be turned upper case in the output.
  
**TODO: more examples to come.**

## TODO list

  * support for input without output
  * support for `stdin` and `stdout`
  * support for external Clojure/Java libraries
  * support TCP sockets
  * add processing statistics

## License

Copyright © 2015 Bruno Bonacci

Distributed under the MIT License.
