/*
    http://www.JSON.org/json2.js
    2011-02-23

    Public Domain.

    NO WARRANTY EXPRESSED OR IMPLIED. USE AT YOUR OWN RISK.

    See http://www.JSON.org/js.html


    This code should be minified before deployment.
    See http://javascript.crockford.com/jsmin.html

    USE YOUR OWN COPY. IT IS EXTREMELY UNWISE TO LOAD CODE FROM SERVERS YOU DO
    NOT CONTROL.


    This file creates a global JSON object containing two methods: stringify
    and parse.

        JSON.stringify(value, replacer, space)
            value       any JavaScript value, usually an object or array.

            replacer    an optional parameter that determines how object
                        values are stringified for objects. It can be a
                        function or an array of strings.

            space       an optional parameter that specifies the indentation
                        of nested structures. If it is omitted, the text will
                        be packed without extra whitespace. If it is a number,
                        it will specify the number of spaces to indent at each
                        level. If it is a string (such as '\t' or '&nbsp;'),
                        it contains the characters used to indent at each level.

            This method produces a JSON text from a JavaScript value.

            When an object value is found, if the object contains a toJSON
            method, its toJSON method will be called and the result will be
            stringified. A toJSON method does not serialize: it returns the
            value represented by the name/value pair that should be serialized,
            or undefined if nothing should be serialized. The toJSON method
            will be passed the key associated with the value, and this will be
            bound to the value

            For example, this would serialize Dates as ISO strings.

                Date.prototype.toJSON = function (key) {
                    function f(n) {
                        // Format integers to have at least two digits.
                        return n < 10 ? '0' + n : n;
                    }

                    return this.getUTCFullYear()   + '-' +
                         f(this.getUTCMonth() + 1) + '-' +
                         f(this.getUTCDate())      + 'T' +
                         f(this.getUTCHours())     + ':' +
                         f(this.getUTCMinutes())   + ':' +
                         f(this.getUTCSeconds())   + 'Z';
                };

            You can provide an optional replacer method. It will be passed the
            key and value of each member, with this bound to the containing
            object. The value that is returned from your method will be
            serialized. If your method returns undefined, then the member will
            be excluded from the serialization.

            If the replacer parameter is an array of strings, then it will be
            used to select the members to be serialized. It filters the results
            such that only members with keys listed in the replacer array are
            stringified.

            Values that do not have JSON representations, such as undefined or
            functions, will not be serialized. Such values in objects will be
            dropped; in arrays they will be replaced with null. You can use
            a replacer function to replace those with JSON values.
            JSON.stringify(undefined) returns undefined.

            The optional space parameter produces a stringification of the
            value that is filled with line breaks and indentation to make it
            easier to read.

            If the space parameter is a non-empty string, then that string will
            be used for indentation. If the space parameter is a number, then
            the indentation will be that many spaces.

            Example:

            text = JSON.stringify(['e', {pluribus: 'unum'}]);
            // text is '["e",{"pluribus":"unum"}]'


            text = JSON.stringify(['e', {pluribus: 'unum'}], null, '\t');
            // text is '[\n\t"e",\n\t{\n\t\t"pluribus": "unum"\n\t}\n]'

            text = JSON.stringify([new Date()], function (key, value) {
                return this[key] instanceof Date ?
                    'Date(' + this[key] + ')' : value;
            });
            // text is '["Date(---current time---)"]'


        JSON.parse(text, reviver)
            This method parses a JSON text to produce an object or array.
            It can throw a SyntaxError exception.

            The optional reviver parameter is a function that can filter and
            transform the results. It receives each of the keys and values,
            and its return value is used instead of the original value.
            If it returns what it received, then the structure is not modified.
            If it returns undefined then the member is deleted.

            Example:

            // Parse the text. Values that look like ISO date strings will
            // be converted to Date objects.

            myData = JSON.parse(text, function (key, value) {
                var a;
                if (typeof value === 'string') {
                    a =
/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)Z$/.exec(value);
                    if (a) {
                        return new Date(Date.UTC(+a[1], +a[2] - 1, +a[3], +a[4],
                            +a[5], +a[6]));
                    }
                }
                return value;
            });

            myData = JSON.parse('["Date(09/09/2001)"]', function (key, value) {
                var d;
                if (typeof value === 'string' &&
                        value.slice(0, 5) === 'Date(' &&
                        value.slice(-1) === ')') {
                    d = new Date(value.slice(5, -1));
                    if (d) {
                        return d;
                    }
                }
                return value;
            });


    This is a reference implementation. You are free to copy, modify, or
    redistribute.
*/

/*jslint evil: true, strict: false, regexp: false */

/*members "", "\b", "\t", "\n", "\f", "\r", "\"", JSON, "\\", apply,
    call, charCodeAt, getUTCDate, getUTCFullYear, getUTCHours,
    getUTCMinutes, getUTCMonth, getUTCSeconds, hasOwnProperty, join,
    lastIndex, length, parse, prototype, push, replace, slice, stringify,
    test, toJSON, toString, valueOf
*/


// Create a JSON object only if one does not already exist. We create the
// methods in a closure to avoid creating global variables.

var JSON;
if (!JSON) {
    JSON = {};
}

(function () {
    "use strict";

    function f(n) {
        // Format integers to have at least two digits.
        return n < 10 ? '0' + n : n;
    }

    if (typeof Date.prototype.toJSON !== 'function') {

        Date.prototype.toJSON = function (key) {

            return isFinite(this.valueOf()) ?
                this.getUTCFullYear()     + '-' +
                f(this.getUTCMonth() + 1) + '-' +
                f(this.getUTCDate())      + 'T' +
                f(this.getUTCHours())     + ':' +
                f(this.getUTCMinutes())   + ':' +
                f(this.getUTCSeconds())   + 'Z' : null;
        };

        String.prototype.toJSON      =
            Number.prototype.toJSON  =
            Boolean.prototype.toJSON = function (key) {
                return this.valueOf();
            };
    }

    var cx = /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
        escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
        gap,
        indent,
        meta = {    // table of character substitutions
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
        },
        rep;


    function quote(string) {

// If the string contains no control characters, no quote characters, and no
// backslash characters, then we can safely slap some quotes around it.
// Otherwise we must also replace the offending characters with safe escape
// sequences.

        escapable.lastIndex = 0;
        return escapable.test(string) ? '"' + string.replace(escapable, function (a) {
            var c = meta[a];
            return typeof c === 'string' ? c :
                '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
        }) + '"' : '"' + string + '"';
    }


    function str(key, holder) {

// Produce a string from holder[key].

        var i,          // The loop counter.
            k,          // The member key.
            v,          // The member value.
            length,
            mind = gap,
            partial,
            value = holder[key];

// If the value has a toJSON method, call it to obtain a replacement value.

        if (value && typeof value === 'object' &&
                typeof value.toJSON === 'function') {
            value = value.toJSON(key);
        }

// If we were called with a replacer function, then call the replacer to
// obtain a replacement value.

        if (typeof rep === 'function') {
            value = rep.call(holder, key, value);
        }

// What happens next depends on the value's type.

        switch (typeof value) {
        case 'string':
            return quote(value);

        case 'number':

// JSON numbers must be finite. Encode non-finite numbers as null.

            return isFinite(value) ? String(value) : 'null';

        case 'boolean':
        case 'null':

// If the value is a boolean or null, convert it to a string. Note:
// typeof null does not produce 'null'. The case is included here in
// the remote chance that this gets fixed someday.

            return String(value);

// If the type is 'object', we might be dealing with an object or an array or
// null.

        case 'object':

// Due to a specification blunder in ECMAScript, typeof null is 'object',
// so watch out for that case.

            if (!value) {
                return 'null';
            }

// Make an array to hold the partial results of stringifying this object value.

            gap += indent;
            partial = [];

// Is the value an array?

            if (Object.prototype.toString.apply(value) === '[object Array]') {

// The value is an array. Stringify every element. Use null as a placeholder
// for non-JSON values.

                length = value.length;
                for (i = 0; i < length; i += 1) {
                    partial[i] = str(i, value) || 'null';
                }

// Join all of the elements together, separated with commas, and wrap them in
// brackets.

                v = partial.length === 0 ? '[]' : gap ?
                    '[\n' + gap + partial.join(',\n' + gap) + '\n' + mind + ']' :
                    '[' + partial.join(',') + ']';
                gap = mind;
                return v;
            }

// If the replacer is an array, use it to select the members to be stringified.

            if (rep && typeof rep === 'object') {
                length = rep.length;
                for (i = 0; i < length; i += 1) {
                    if (typeof rep[i] === 'string') {
                        k = rep[i];
                        v = str(k, value);
                        if (v) {
                            partial.push(quote(k) + (gap ? ': ' : ':') + v);
                        }
                    }
                }
            } else {

// Otherwise, iterate through all of the keys in the object.

                for (k in value) {
                    if (Object.prototype.hasOwnProperty.call(value, k)) {
                        v = str(k, value);
                        if (v) {
                            partial.push(quote(k) + (gap ? ': ' : ':') + v);
                        }
                    }
                }
            }

// Join all of the member texts together, separated with commas,
// and wrap them in braces.

            v = partial.length === 0 ? '{}' : gap ?
                '{\n' + gap + partial.join(',\n' + gap) + '\n' + mind + '}' :
                '{' + partial.join(',') + '}';
            gap = mind;
            return v;
        }
    }

// If the JSON object does not yet have a stringify method, give it one.

    if (typeof JSON.stringify !== 'function') {
        JSON.stringify = function (value, replacer, space) {

// The stringify method takes a value and an optional replacer, and an optional
// space parameter, and returns a JSON text. The replacer can be a function
// that can replace values, or an array of strings that will select the keys.
// A default replacer method can be provided. Use of the space parameter can
// produce text that is more easily readable.

            var i;
            gap = '';
            indent = '';

// If the space parameter is a number, make an indent string containing that
// many spaces.

            if (typeof space === 'number') {
                for (i = 0; i < space; i += 1) {
                    indent += ' ';
                }

// If the space parameter is a string, it will be used as the indent string.

            } else if (typeof space === 'string') {
                indent = space;
            }

// If there is a replacer, it must be a function or an array.
// Otherwise, throw an error.

            rep = replacer;
            if (replacer && typeof replacer !== 'function' &&
                    (typeof replacer !== 'object' ||
                    typeof replacer.length !== 'number')) {
                throw new Error('JSON.stringify');
            }

// Make a fake root object containing our value under the key of ''.
// Return the result of stringifying the value.

            return str('', {'': value});
        };
    }


// If the JSON object does not yet have a parse method, give it one.

    if (typeof JSON.parse !== 'function') {
        JSON.parse = function (text, reviver) {

// The parse method takes a text and an optional reviver function, and returns
// a JavaScript value if the text is a valid JSON text.

            var j;

            function walk(holder, key) {

// The walk method is used to recursively walk the resulting structure so
// that modifications can be made.

                var k, v, value = holder[key];
                if (value && typeof value === 'object') {
                    for (k in value) {
                        if (Object.prototype.hasOwnProperty.call(value, k)) {
                            v = walk(value, k);
                            if (v !== undefined) {
                                value[k] = v;
                            } else {
                                delete value[k];
                            }
                        }
                    }
                }
                return reviver.call(holder, key, value);
            }


// Parsing happens in four stages. In the first stage, we replace certain
// Unicode characters with escape sequences. JavaScript handles many characters
// incorrectly, either silently deleting them, or treating them as line endings.

            text = String(text);
            cx.lastIndex = 0;
            if (cx.test(text)) {
                text = text.replace(cx, function (a) {
                    return '\\u' +
                        ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
                });
            }

// In the second stage, we run the text against regular expressions that look
// for non-JSON patterns. We are especially concerned with '()' and 'new'
// because they can cause invocation, and '=' because it can cause mutation.
// But just to be safe, we want to reject all unexpected forms.

// We split the second stage into 4 regexp operations in order to work around
// crippling inefficiencies in IE's and Safari's regexp engines. First we
// replace the JSON backslash pairs with '@' (a non-JSON character). Second, we
// replace all simple value tokens with ']' characters. Third, we delete all
// open brackets that follow a colon or comma or that begin the text. Finally,
// we look to see that the remaining characters are only whitespace or ']' or
// ',' or ':' or '{' or '}'. If that is so, then the text is safe for eval.

            if (/^[\],:{}\s]*$/
                    .test(text.replace(/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g, '@')
                        .replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']')
                        .replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {

// In the third stage we use the eval function to compile the text into a
// JavaScript structure. The '{' operator is subject to a syntactic ambiguity
// in JavaScript: it can begin a block or an object literal. We wrap the text
// in parens to eliminate the ambiguity.

                j = eval('(' + text + ')');

// In the optional fourth stage, we recursively walk the new structure, passing
// each name/value pair to a reviver function for possible transformation.

                return typeof reviver === 'function' ?
                    walk({'': j}, '') : j;
            }

// If the text is not JSON parseable, then a SyntaxError is thrown.

            throw new SyntaxError('JSON.parse');
        };
    }
}());
function on_focus(){
	//alert('test');
	var elements = new Array('training_name','lous', '', 'hour_from');

   for (x in elements){
       //alert (elements[x]);
       //document.getElementById(elements[x]).focus()
  
         switch(elements[x]){
        case 'lous':
        if (document.getElementById('lous')){
          document.getElementById('lous').focus();
        }
          break;
        case 'training_name':
        if (document.getElementById('training_name')){
          document.getElementById('training_name').focus();
        }
          break;
        case 'department':
        if (document.getElementById('department')){
          document.getElementById('department').focus();
        }
          break;
        case 'hour_from':
        if (document.getElementById('hour_from')){
          document.getElementById('hour_from').focus();
        }
          break;
        }
   } 
}
<!-- Installation & Deliveries -->

function trim(stringToTrim) {
	return stringToTrim.replace(/^\s+|\s+$/g,"");
}

<!-- Installation & Deliveries -->

function if_empty (firstV, secV)
{

 if ( firstV == "" || firstV == "none" )
 {
 	document.getElementById(secV).innerHTML="!!!";
 	return "1C";
 	}  	
 else
  {
  document.getElementById(secV).innerHTML="&nbsp;&nbsp;&nbsp;";
  return "0C";
  }	
}
function trainingMsg (firstV, secV)
{
	document.getElementById(secV).innerHTML = firstV;
}//Globals
var btnWhichButton;

function Login_Tr_Check(thForm)
{
var LoginVal, PasswordVal;

var LoginRaw = trim(document.getElementById("lous").value);
var PasswordRaw = trim(document.getElementById("lpas").value);



switch(btnWhichButton.name)
{
case "Enter":
  LoginVal = if_empty (LoginRaw, "lousExc");
  PasswordVal = if_empty (PasswordRaw, "lpasExcl" );  
  break;          
}

if ( LoginVal == "1C" )
  {
  	thForm.lous.focus();
  	return false ; 	
  }    
else if ( PasswordVal == "1C" )
  {
  	thForm.lpas.focus();
  	return false ; 	
  }
    
}
var btnDateButton;

function date_Tr_Check(thForm){

 TrainingRaw = trim(document.getElementById("trainings").value);
 DateRaw = trim(document.getElementById("tr_date").value);
 TimeFromH = trim(document.getElementById("hour_from").value); 
 TimeFromM = trim(document.getElementById("min_from").value);
 TimeToH = trim(document.getElementById("hour_to").value); 
 TimeToM = trim(document.getElementById("min_to").value);
 id_trainRaw = trim(document.getElementById("TRAINDATE_ID").value);
 Train_place = trim(document.getElementById("train_place").value);
      
    
    if (TimeFromH == "none"){
    	TimeFrom = "none";
    }
    else if (TimeFromM == "none"){
    	TimeFrom = "none";
    }
    else {
    	TimeFrom = TimeFromH + TimeFromM;
    }
    
    if (TimeToH == "none"){
    	TimeTo = "none";
    }
    else if (TimeToM == "none"){
    	TimeTo = "none";
    }
    else {
    	TimeTo = TimeToH + TimeToM;
    }
    
    if (TimeFrom >= TimeTo){
    	TimeTo = "none";
    }
    
  switch(btnDateButton.name){
  case "add_date":
    TrainingVal = if_empty (TrainingRaw, "trainingExcl" );
    DateVal = if_empty (DateRaw, "dates_jsExcl" );  
    TimeFromVal = if_empty (TimeFrom, "hour_fromMsg" ); 
    TimeToVal = if_empty (TimeTo, "hour_toMsg" ); 
    Train_placeVal = if_empty(Train_place, "place_jsExcl");
    id_trainVal = "0C";
    break;
  case "update_date":
    TrainingVal = if_empty (TrainingRaw, "trainingExcl" );
    DateVal = if_empty (DateRaw, "dates_jsExcl" );  
    TimeFromVal = if_empty (TimeFrom, "hour_fromMsg" ); 
    TimeToVal = if_empty (TimeTo, "hour_toMsgp" ); 
    Train_placeVal = if_empty(Train_place, "place_jsExcl");  
    id_trainVal = if_empty (id_trainRaw, "TRAINDATE_IDExcl" );   
    break;
  case "list_dates":
    return true;  
    break;            
  }
      

  if(DateVal == "1C"){
    	return false ; 	
  }
  else if(Train_placeVal == "1C"){
    return false; 	
  }
  else if(TrainingVal == "1C"){
    return false ; 	
  }
  else if(TimeFromVal == "1C"){
    return false ; 	
  }
  else if(TimeToVal == "1C"){
    return false ; 	
  }
  else if(id_trainVal == "1C"){
    return false ; 	
  }      
  
}
//Globals
var btnTrainerButton;

function trainer_Tr_Check(thForm)
{
var Trainer_NameVal, Training_idVal, id_trainerVal;

var Trainer_NameRaw = trim(document.getElementById("trainer_name").value);
var Training_idRaw = trim(document.getElementById("TID").value);
var id_trainerRaw = trim(document.getElementById("TRAINERS_ID").value);


switch(btnTrainerButton.name)
{
case "insert_trainers":
  Trainer_NameVal = if_empty (Trainer_NameRaw, "trainer_nameExc");
  Training_idVal = if_empty (Training_idRaw, "TIDExc" );  
  break;
case "update_trainers":
  Trainer_NameVal = if_empty (Trainer_NameRaw, "trainer_nameExc");
  Training_idVal = if_empty (Training_idRaw, "TIDExc" );  
  id_trainerVal = if_empty (id_trainerRaw, "TRAINERS_IDExcl" );   
  break;
case "list_trainers":
  return true;  
  break;            
}

if ( Trainer_NameVal == "1C" )
  {
  	thForm.trainer_name.focus();
  	return false ; 	
  }    
else if ( Training_idVal == "1C" )
  {
  	thForm.TID.focus();
  	return false ; 	
  }
else if ( id_trainerVal == "1C" )
  {
  	thForm.TRAINERS_ID.focus();
  	return false ; 	
  }
    
}
//Globals
var btnTrainingButton;

function trainings_Tr_Check(thForm)
{
var Training_full_NameVal, Training_short_NameVal, id_trainingVal;

var Training_full_NameRaw = trim(document.getElementById("training_name").value);
var Training_short_NameRaw = trim(document.getElementById("training_sname").value);
var id_trainingRaw = trim(document.getElementById("TRAININGS_ID").value);


switch(btnTrainingButton.name)
{
case "insert_trainings":
  Training_full_NameVal = if_empty_full (Training_full_NameRaw, "training_nameExc", "trainingmsg");
  Training_short_NameVal = if_empty_short (Training_short_NameRaw, "training_snameExc", "trainingmsg" );
  radiobtns = if_empty_group ('G','T','trainingmsg');
	
  break;
case "update_trainings":
  Training_full_NameVal = if_empty_full (Training_full_NameRaw, "training_nameExc", "trainingmsg");
  Training_short_NameVal = if_empty_short (Training_short_NameRaw, "training_snameExc", "trainingmsg" );  
  id_trainingVal = if_empty (id_trainingRaw, "TRAININGS_IDExc" ); 
  radiobtns = if_empty_group ('G','T','trainingmsg');
  break;
case "list_trainings":
  return true;  
  break;            
}
//alert (Training_full_NameVal);
if ( Training_full_NameVal == "1C" )
  {
  	thForm.training_name.focus();
  	return false ;
  }    
else if ( Training_short_NameVal == "1C" )
  {
  	thForm.training_sname.focus();
  	return false ; 	
  }
else if ( id_trainingVal == "1C" )
  {
  	thForm.TRAININGS_ID.focus();
  	return false ; 	
  }
else if ( radiobtns == "1C" )
  {
  	thForm.G.focus();
  	return false ; 	
  }
  
    
}//Globals
var btnAddButton;

function add_Tr_Check(adForm)
{
var idVal;

var idRaw = trim(document.getElementById("person_id").value);

switch(btnAddButton.name)
{
case "sbm_person":
  idVal = if_empty (idRaw, "person_idExcl");
  break;          
}

if ( idVal == "1C" )
  {
  	adForm.person_id.focus();
  	return false ; 	
  }    
   
}function setFieldStyle(x,y)
{
	switch (y)
	{
		case 0:
		 document.getElementById(x).style.background="#E0E0E0";
		  break;
	  case 1:
	   document.getElementById(x).style.background="#FFFFFF";
	    break;
	}    	   
}  //Globals
   var evo_id;	
function only_one_Visibility(id) 
 {
  if(evo_id)
   {
  	
		if ( document.getElementById(evo_id).style.display == 'inline' )
		 { document.getElementById(evo_id).style.display = 'none';
		 	}
		else
		 { document.getElementById(id).style.display = 'none';} 
   }
  	
   evo_id=id;
   //alert (evo_id);
  	    if ( document.getElementById(id).style.display == 'none' )
		       { document.getElementById(id).style.display = 'inline';}
		    else
		      { document.getElementById(id).style.display = 'none';} 		 	
}function idAutoFit (firstValue, elName)
{	document.getElementById(elName).value = firstValue; }
//function fitSomeData(thForm, RawStr)
//{
//	
//	  
//	var trainers;
//	var tr_email;
//	
//	RawStr=RawStr.value;
//	trainers=RawStr.split("~SePaRaToR_2~");
//	trainersList="Trainer:<br /><SELECT name=\"trainers_pull\" ID=\"trainers_pull\" class=\"tr_pull_menu\"  >";
//	   
//	  for ( var i in trainers ){
//	  	
//      tr_email=trainers[i].split("~SePaRaToR_1~");
//      
//           trainersList=trainersList+"<option value=\""+tr_email[1]+"\">"+tr_email[0]+"<\/option>";
//         
//    } 
//  trainersList=trainersList+"<\/SELECT>";
//	document.getElementById('trPull').innerHTML = trainersList;
//  //document.getElementById('hidden_train').value = tr_email[2];
//  document.getElementById('hidden_id').value = tr_email[2];
//         	    
//	
//	
//}

//HRCENTER-1214 HRCENTER-1245
function getTrainers() {
	var id = document.getElementById('trainings').value;
	$.ajax({
		  method: "GET",
		  url: WEB_ROOT+'fr/ajax/get-trainers?id='+id,
		  dataType: 'json'
	}).done(function(response) {
		document.getElementById('hidden_id').value = id;
		dataTrainers(response.data, 'train_person2', 'part');
		dataTrainers(response.data, 'train_person3', 'part');
		dataTrainers(response.data, 'trainers_pull', '');
	});
}

//HRCENTER-1214 HRCENTER-1245
function dataTrainers(trainersData, idElement, flag) {
	var select = document.getElementById(idElement);
	select.innerHTML = '';
	
	if (flag == 'part') {
		var defaultOpt = document.createElement('option');
		defaultOpt.value = '';
		defaultOpt.innerHTML = 'none';
		select.appendChild(defaultOpt);
	}
	
	for (var i in trainersData){
	      var opt = document.createElement('option');
	      opt.value = trainersData[i]['USER_ID_TRAINER'];
	      opt.innerHTML = trainersData[i]['FNAME'] + " " + trainersData[i]['LNAME'];
	      select.appendChild(opt);
	  }
}function popUp(URL, windowname)
{
   var tr_name, tr_URL;

     tr_name = windowname;
     tr_URL=URL+"?TRAIN_ID="+tr_name;
     //alert (tr_URL);
window.open(tr_URL, "windowname", 'width=650,height=770,left=500,top=100,scrollbars=1');

}

function popUp2(URL,id)
{
	 URL=URL+"?ID="+id;
   window.open(URL, 'Details', 'width=400,height=250,left=500,top=100,scrollbars=no'); 

}


function popUp3(URL,id, module){
	
   if(module){
	   URL=URL+"?"+module+"&ID="+id; 
   } else {
	   URL=URL+"?ID="+id;  
   }	

   window.open(URL, 'Details', 'width=250,height=150,left=500,top=100,scrollbars=no'); 

}

function popUp4(URL,id,cell)
{
	 URL=URL+"?ID="+id+"&CELL="+cell;
   window.open(URL, 'Details', 'width=250,height=150,left=500,top=100,scrollbars=no'); 

}
function popUp_cal(URL)
{	 
   window.open(URL, 'Details', 'width=1100,height=850,left=100,top=100,scrollbars=yes'); 

}
function popUp5(URL, idu)
{
   var id_user, tr_URL;

     id_user = idu;
     tr_URL=URL+"?idu="+idu;
     //alert (tr_URL);
window.open(tr_URL, "idu", 'width=550,height=370,left=500,top=100,scrollbars=1');

}
function popUp6(URL)
{
   var tr_URL;

     tr_URL=URL;
     //alert (tr_URL);
window.open(tr_URL, "request_training", 'width=700,height=420,left=500,top=100,location=0,status=0,directories=0,menubar=0,resizable=1,scrollbars=1');

}
function popUp7(URL)
{
   var  tr_URL;
   var  date          = parent.document.getElementById("tr_date").value;
   var  hourfrom      = parent.document.getElementById("hour_from").value;
   var  min_from      = parent.document.getElementById("min_from").value;
   var  hourto        = parent.document.getElementById("hour_to").value;
   var  min_to        = parent.document.getElementById("min_to").value;
   var  train_place   = parent.document.getElementById("train_place").value;
   var  trainer       = parent.document.getElementById("trainers_pull").value;
   
     tr_URL=URL;
     //alert (tr_URL);
     tr_URL=URL+"&date="+date+"&hourfrom="+hourfrom+"&min_from="+min_from+"&hourto="+hourto+"&min_to="+min_to+"&train_place="+train_place+"&trainer="+trainer;
   
window.open(tr_URL, "request_training", 'width=460,height=370,left=500,top=100,location=0,status=0,directories=0,menubar=0,scrollbars=0');

}function popUp_window(URL, windowname)
{
   var tr_name, tr_URL;

     tr_name = windowname;
     tr_URL=URL+"?TRAIN_NAME="+tr_name;
     //alert (tr_URL);
     window.open(tr_URL, "windowname", 'left=300,top=100,width=520,height=600,scrollbars=0,menubar=0,status=0,toolbar=0,directories=0,location=0');

}
//Globals
var btnWhichButton;
var preventId=1;
var preventAnother=1;
var Oldvalue;
var focusS;
 
  
function maCel( FormName, cellNumber,  butId, cases, whatVall,trID )
{
	
var id_handel;
var varDOM;

 varDOM=separatiring(whatVall);

    
if ( preventAnother < 2 ) {
	 preventAnother++;
	 preventId=cellNumber 
	 //alert ('Stage 1  '+preventAnother);
  }
else if ( preventAnother == 2 && preventId!=cellNumber ) {
	preventAnother=1;
	//alert ('Stage 2  '+preventAnother);
  }  
else if ( preventAnother == 2 && preventId==cellNumber ) {
	Oldvalue=document.getElementById(cellNumber).innerHTML;
	//alert (Oldvalue);
	//document.getElementById(cellNumber).innerHTML = "<input type=text size=10 value=dd name=fname id=\"handel\" />";	
	document.getElementById(cellNumber).innerHTML = varDOM;
	//alert (varDOM);
	preventAnother++;
	//alert ('Stage 3  '+preventAnother);
  }
else if ( preventAnother == 3 && preventId!=cellNumber){
	document.getElementById(preventId).innerHTML = Oldvalue;
	preventAnother=1;
	//alert ('Stage 4  '+preventAnother);
  }   
else if ( preventAnother == 3 && preventId==cellNumber){
	preventAnother++;
  } 
else if ( preventAnother == 4 && preventId!=cellNumber){
	document.getElementById(preventId).innerHTML = Oldvalue;
	preventAnother=1;
  }     
else if ( preventAnother == 4 && preventId==cellNumber){
	
	
	switch(cases)
{
case 1:
  document.getElementById('ATEND1'+trID).value = document.getElementById('handel').value
  break;    
case 2:
  document.getElementById('ATEND2'+trID).value = document.getElementById('handel').value
  break;
case 3:
  document.getElementById('ATEND3'+trID).value = document.getElementById('handel').value
  break;
case 4:
  document.getElementById('TEST1'+trID).value = document.getElementById('handel').value
  break;
case 5:
  document.getElementById('TEST2'+trID).value = document.getElementById('handel').value
  break;  
case 6:
  document.getElementById('FINAL'+trID).value = document.getElementById('handel').value
  break;  
}
  document.getElementById('Row'+trID).value = butId;
  document.getElementById('TRAIN'+trID).value = trID;
  
	FormName.submit();

  //startReload();

  }
 
}
function separatiring (whatVall)
 {
 var varMadePull	
  	if (whatVall)
  {
  	var arrOpt=whatVall.split("-SePaRaToR-");
  	//alert(arrOpt[2]);
  	
  	
  	for (keyVar in arrOpt) {
  		if (varMadePull)
  		 {varMadePull=varMadePull+"<OPTION value=\""+arrOpt[keyVar]+"\"> "+arrOpt[keyVar]+" <\/OPTION>";}
  		else
  		 {varMadePull="<SELECT name=\"fname\" ID=handel > <OPTION selected value=\""+arrOpt[0]+"\">"+arrOpt[0]+"<\/OPTION>";}
    //alert(arrOpt[keyVar]);
 }   
    varMadePull=varMadePull+"<OPTION value=\"clear\"> clear <\/OPTION><\/SELECT>";
  } 
 return varMadePull;	
}function switch_background(id , URL1, URL2){
	  
	  URL_BROWSER = document.getElementById(id).style.backgroundImage;
    URL_BROWSER = URL_BROWSER.replace('"','');
    URL_BROWSER = URL_BROWSER.replace('"','');
    
    var check = URL_BROWSER.search(/http:/i);
    
    if( check != -1 ){
      path = "http://"+location.host+""+location.pathname;
      path = path.replace("index.php", "");
      URL1 = "url("+path+""+URL1+")";
      URL2 = "url("+path+""+URL2+")";
    }
    else{
    	URL1 = "url("+URL1+")";
    	URL2 = "url("+URL2+")";
    }
    
		if (URL_BROWSER == URL1 ){ 
		 	 document.getElementById(id).style.backgroundImage = URL2;
		 	//check if exist this DOM object
		 	 var hidden_input = document.getElementById('hidden_input');
		 	 if (hidden_input != null) {
		 		hidden_input.value = 1;
		 	 }
		 	 
		}
		else if(URL_BROWSER == URL2 ){ 
				document.getElementById(id).style.backgroundImage = URL1;
				//check if exist this DOM object
				 var hidden_input = document.getElementById('hidden_input');
			 	 if (hidden_input != null) {
			 		hidden_input.value = 0;
			 	 }
		}			 			 	
}
function switch_background2(id , URL){
		 document.getElementById(id).style.backgroundImage = URL;
		 document.getElementById(id).title = "update";
}
<!-- Installation & Deliveries -->

function if_empty_full (firstV, secV, tirdV)
{

 if ( firstV == "" )
 {
 	document.getElementById(secV).innerHTML="!!!";
 	document.getElementById(tirdV).innerHTML="<font color= #F00>Please type full name<\/font>";
 	return "1C";
 	}  	
 else
  {
  	document.getElementById(secV).innerHTML="&nbsp;&nbsp;&nbsp;";
    document.getElementById(tirdV).innerHTML="";
  return "0C";
  }	
}
function if_empty_short (firstV, secV, tirdV)
{

 if ( firstV == "" )
 {
 	  if (document.getElementById(tirdV).innerHTML == ''){
 	  	  document.getElementById(secV).innerHTML="!!!";
 	      document.getElementById(tirdV).innerHTML="<font color= #F00>Please type short name<\/font>";}
 	return "1C";
 	}  	
 else
  {
    document.getElementById(secV).innerHTML="&nbsp;&nbsp;&nbsp;";
    document.getElementById(tirdV).innerHTML="";
  return "0C";
  }	
}
function if_empty_group (firstV, secV , tirdV)
{
	radio1 = document.getElementById(firstV).checked;
	radio2 = document.getElementById(secV).checked;
	//alert (radio1);
	//alert (radio2);
	    if ( radio1 == false && radio2 == false){
	    	 if (document.getElementById(tirdV).innerHTML == ''){
	    	document.getElementById(tirdV).innerHTML="<font color= #F00>Please select group<\/font>";}
	    	return "1C";
	    }
	    else {
	    	return "0C";
	    }
}function id_multi_select (firstValue, elName, person_id)
{

            if (!document.getElementById(person_id).value){
                document.getElementById(person_id).value =  firstValue;      
            }
            else {
                document.getElementById(person_id).value = firstValue+";"+document.getElementById(person_id).value;
                 }	

}<!-- Installation & Deliveries -->

function change_value (val1, id1 , val2, id2)
{ //alert (id1);
	var element1 = document.getElementById(id1);
	if (element1 != null) {
		document.getElementById(id1).value = val1;
	}
	
	var element2 = document.getElementById(id2);
	if (element2 != null) {
		document.getElementById(id2).value = val2;
	}
	
	
}

function change_innerHTML (val1, id1 , val2, id2)
{

	document.getElementById(id1).innerHTML = val1;
	document.getElementById(id2).innerHTML = val2;
	
}<!-- Installation & Deliveries -->

function check_tr_place(place_string)
{
	 var massage;
	 var msg;
  //alert(place_string);
  places = place_string.split("~SePaRaToR~");
		for ( var i in places ){
			second_loop = places[i].split(",");
						
	  	Tr_date = document.getElementById('tr_date').value;
	  	hour_from = document.getElementById('hour_from').value;
	  	min_from = document.getElementById('min_from').value;
	  	Hour_from = hour_from +":"+ min_from+":00";

	  	hour_to = document.getElementById('hour_to').value;
	  	min_to = document.getElementById('min_to').value; 
	  	Hour_to = hour_to +":"+ min_to+":00";
	  	
	  	
	  	train_place = document.getElementById('train_place').value;
	  	id = document.getElementById('TRAINDATE_ID').value;  
	  	
	  	rowID = second_loop[0];
	  	//alert (rowID);
	  	//alert(id);
	  	time_from = second_loop[1];  
	  	time_to = second_loop[2];
	  	dates = second_loop[3];
	  	place = second_loop[4];
	  	
	  	
	  	if (id != rowID){
	  	   
	  	   if (Hour_from >= time_from && Hour_from < time_to && Tr_date == dates && train_place == place){
	  	   	
	  	   	time_from = time_from.split(":");
	  	   	time_to = time_to.split(":");
	  	   	//dates = dates.split("-");
	  	   	msg = ('The place is not free on '+ convert_date(dates) +' from '+time_from[0]+':'+time_from[1]+'h to '+time_to[0]+':'+time_to[1]+'h.');
	  	    //alert(msg);
	  	   }
	  	   else if(Hour_to > time_from && Hour_to < time_to && Tr_date == dates && train_place == place){
	  	   	time_from = time_from.split(":");
	  	   	time_to = time_to.split(":");
	  	   	//dates = dates.split("-");
	  	   	msg = ('The place is not free on '+ convert_date(dates) +' from '+time_from[0]+':'+time_from[1]+'h to '+time_to[0]+':'+time_to[1]+'h.');
	  	    //alert(msg);
	  	   }
	  	   else if(Hour_from < time_from && Hour_to > time_to && Tr_date == dates && train_place == place){
	  	   	time_from = time_from.split(":");
	  	   	time_to = time_to.split(":");
	  	   	//dates = dates.split("-");
	  	   	msg = ('The place is not free on '+ convert_date(dates) +' from '+time_from[0]+':'+time_from[1]+'h to '+time_to[0]+':'+time_to[1]+'h.');
	  	    //alert(msg);
	  	   }
      
            if (msg){
            	 if (!massage){
            	 massage = msg+"\n";
            	 msg = '';
            	 }
            	 else if(massage){
               massage = massage + msg+"\n";
               msg = '';
               }
               
            }
      
      } 
  }
   if (massage){
    alert (massage);
   }
}

function convert_date (d)
{
	dates = d.split("-");
	
	day = dates[2];
	month = dates[1];
	year = dates[0];
	
	 switch(month){
        	case '01':
        		month_name = 'January';
        		break;
        	case '02':
        		month_name = 'February';
        		break;
        	case '03':
        		month_name = 'March';
        		break;
        	case '04':
        		month_name = 'April';
        		break;
        	case '05':
        		month_name = 'May';
        		break;
        	case '06':
        		month_name = 'June';
        		break;
        	case '07':
        		month_name = 'July';
        		break;
        	case '08':
        		month_name = 'August';
        		break;
        	case '09':
        		month_name = 'September';
        		break;
        	case '10':
        		month_name = 'October';
        		break;
        	case '11':
        		month_name = 'November';
        		break;
        	case '12':
        		month_name = 'December';
        		break;											
        }	
     return day+" "+month_name+" "+year;
	
}var btnWhichButton;

function alert_msg(FormName) 
{ 
		
  if (btnWhichButton){

	//alert(FormName);
	alert_msg = confirm("Do you realy want to remove this person?"); 
 
   if (alert_msg == true){
   	 //alert(alert_msg);
     	return true;
   }
   else if(alert_msg == false){
   	// alert(alert_msg);
      document.getElementById('delete').value = 'false';
      document.getElementById('hidden_user_id').value = '';
      document.getElementById('hidden_train_id').value = '';
      FormName.submit;
    }
  }
}

function delete_alert(name){ 
	
	value = "<div style=\"text-align: left;padding-left:10px;\" >";
	value = value + "<label>Description:<\/label>";
	value = value + "<span id=\"description\" class=\"bad\">&nbsp;&nbsp;&nbsp;<\/span><br\/>";
	value = value + "<\/div>";
	value = value + "<textarea id=\"text\" name=\"text\" style=\"width:200px;height:80px;\" ><\/textarea><br\/><br\/>";
	value = value + "<input type=\"submit\" name=\""+name+"\" value=\"submit\" \/>&nbsp;&nbsp;";
	value = value + "<input type=\"button\" name=\"close\" value=\"Close\" onclick=\"return cClick();\" \/>";
	
	document.getElementById('alert_msg_div').innerHTML = value;
}
function check_text(){ 
	value = document.getElementById('text').value;
	if(value ==''){
		document.getElementById('description').innerHTML = "!!!";
		return false;
	}
	else{
		return true;
	}
}function SaveScrollXY(form) {
  //alert (form);
  //alert( document.documentElement.scrollTop + document.body.scrollTop);
  form.hidden_scroll_top.value = document.documentElement.scrollTop + document.body.scrollTop;  
}

function ResetScrollPosition(hidx, hidy) {
  //alert(hidx);
  //alert(hidy);
  window.scrollTo(hidy, hidx);
}

function getPageScroll() {
	var yScroll;
	if (self.pageYOffset) {
	yScroll = self.pageYOffset;
	} else if (document.documentElement && document.documentElement.scrollTop) {
	yScroll = document.documentElement.scrollTop;
	} else if (document.body) {
	yScroll = document.body.scrollTop;
	}
	return yScroll;
}  function createXMLHttp(){
    
    if (window.XMLHttpRequest){
      return new XMLHttpRequest();
    }
    else if(window.ActiveXObject){
    
      var aVersions = ["MSXML2.XMLHttp.5.0",
                       "MSXML2.XMLHttp.4.0",
                       "MSXML2.XMLHttp.3.0",
                       "MSXML2.XMLHttp",
                       "Microsoft.XMLHttp"];
      
      for(var i in aVersions){
        
        try{
          xmlhttp = new ActiveXObject(aVersions[i]);
          return xmlhttp;
        }
        catch(oError){
          
        }
        
      }
 
    }
    
  }function isArray(obj) {
  //returns true is it is an array
  if (obj.constructor.toString().indexOf('Array') == -1){
    return false;
  }
  else{
    return true;
  }
}function setVisibility(id) 
  {

  	//alert (document.getElementById(id).style.display);
		if ( document.getElementById(id).style.display == 'none' )
		 { document.getElementById(id).style.display = 'inline';
		 	//alert (document.getElementById(id).style.display);
		 	}
		else
		 { document.getElementById(id).style.display = 'none';} 
		 
   }

function setVisibility2(id) 
{
  	
  	//alert (document.getElementById(id).style.display);
		if ( document.getElementById(id).style.display == 'none' )
		 { document.getElementById(id).style.display = 'inline';
		 	//alert (document.getElementById(id).style.display);
		 	}
		else
		 { document.getElementById(id).style.display = 'inline';
		 		//alert (document.getElementById(id).style.display);
		 		} 
		 
 }
 
 function setVisibilityTable(id){
    
		if(document.getElementById(id).style.display == 'none' ){ 
			document.getElementById(id).style.display = 'block';
		}
		else{
			document.getElementById(id).style.display = 'none';
		} 
		 
 }
function validate_overtime( FormName, id ,check)
{
  //alert(id);
  //alert(FormName);
  //alert(document.getElementById(checkId).value);
  //alert(check);
  
  if (check == 'yes'){
  document.getElementById('hidden_id').value = id;
  document.getElementById('hidden_validate').value = 'no';
  }
  else{
  document.getElementById('hidden_id').value = id;
  document.getElementById('hidden_validate').value = 'yes';
  }
  
  FormName.submit();
  
}
function checkAll(boxes,person_id,month,year,check_box){
	//alert(boxes);
	//alert(person_id);
	//alert(month);
	//alert(year);
	//alert(check_box);
	//
	//alert(check_box.checked);
	
if(check_box.checked == true){
   alert_msg = confirm("Do you realy want to validate all records for this person?");
   if (alert_msg == true){
  	 for (i=0; i<boxes.length; i++){
       boxes[i].checked = true;
     }
     document.multi_report.hidden_validate.value = 'yes';
     document.multi_report.hidden_person_id.value = person_id;
     document.multi_report.hidden_year.value = year;
     document.multi_report.hidden_month.value = month;
     document.multi_report.submit();
   }
   else{
     check_box.checked = false;	
   }
}
else{
   alert_msg = confirm("Do you realy want to uncheck all records for this person?");
   if (alert_msg == true){
     for (i=0; i<boxes.length; i++){
       boxes[i].checked = false;
     }
     document.multi_report.hidden_validate.value = 'no';
     document.multi_report.hidden_person_id.value = person_id;
     document.multi_report.hidden_year.value = year;
     document.multi_report.hidden_month.value = month;
     document.multi_report.submit();
   }
   else{
     check_box.checked = true;	
   }
}
  
}

function check_checkboxes(boxes,box){
	   
	var check = 0;
	   
  if(boxes.length){ 
	   for (i=0; i<boxes.length; i++){
	  
       if(boxes[i].checked == false){
         	check++;
       }
     }
  }
  else if(boxes.checked	 == false){
     check = 1;
  }
     //alert(check);
     if(check == 0){
     	 box.checked = true;
     }
     else{
       box.checked = false;	
     }
    
}
function checkAll_dept(dept,year,month){
        	
if(document.multi_report.check_all.checked == true){
   alert_msg = confirm("Do you realy want to validate all records for the team?");
   if (alert_msg == true){
  	 
     document.multi_report.hidden_id.value = dept;
     document.multi_report.hidden_year.value = year;
     document.multi_report.hidden_month.value = month;
     document.multi_report.submit();
   }
   else{
     document.multi_report.check_all.checked = false;	
   }
}
else{
   alert_msg = confirm("Do you realy want to uncheck all records for the team?");
   if (alert_msg == true){
     
     document.multi_report.hidden_id.value = dept;
     document.multi_report.hidden_year.value = year;
     document.multi_report.hidden_month.value = month;
     document.multi_report.submit();
   }
   else{ 
    document.multi_report.check_all.checked = true;
   }
}
}


function chechTimeFromTo(hour_from,sel_min_from,hour_to,sel_min_to){
	var return_value = true;
 	if(document.getElementById(hour_from).value == null || document.getElementById(sel_min_from).value == null  || document.getElementById(hour_to).value == null || document.getElementById(sel_min_to).value == null){
		 
	 }else{
		
		var get_hour_from =  parseInt(document.getElementById(hour_from).value,10);
		var get_min_from =  parseInt(document.getElementById(sel_min_from).value,10);
		var get_hour_to =  parseInt(document.getElementById(hour_to).value,10);
		var get_min_to =  parseInt(document.getElementById(sel_min_to).value,10);
		
		var hour_from_in_minute = get_hour_from * 60;
	    var hour_to_in_minute =  get_hour_to * 60;
	    
	    var calc_from = hour_from_in_minute + get_min_from;
	    var calc_to = hour_to_in_minute + get_min_to;

	    if(isNaN(calc_from) || isNaN(calc_to) ){
	        alert('Please select hours');
	        return false;
	    }
	    
	    var time_from = get_hour_from +':'+get_min_from;	    
	    var time_to = get_hour_to +':'+get_min_to;
	    

		 	
		 if ( calc_from > calc_to  ) {
	    	alert('Choosen wrong time from to !!! \n'+time_from+" > "+time_to);
	    	document.getElementById(hour_from+'_warning').style.display = 'inline';
	    	document.getElementById(hour_to+'_warning').style.display = 'inline';
	    	return_value = false;			    	
		 } else{				
	    	document.getElementById(hour_from+'_warning').style.display = 'none';
	    	document.getElementById(hour_to+'_warning').style.display = 'none';
		 }
			 
		//alert(get_hour_from);
		//alert(get_min_from);
		//alert(get_hour_to);
		//alert(get_min_to);	    
	   // console.log(calc_from +' '+ calc_to);
		
	 }
 	
 	
 	return return_value;
 	
	
	
}



//Globals
var btnOvertimeButton;

function overtime_f_Tr_Check(thForm)
{
 DateRaw = trim(document.getElementById("insert_date").value);
 TimeFromH = trim(document.getElementById("hour_from").value); 
 TimeFromM = trim(document.getElementById("sel_min_from").value);
 TimeToH = trim(document.getElementById("hour_to").value); 
 TimeToM = trim(document.getElementById("sel_min_to").value);
 id_issueRaw = trim(document.getElementById("issue").value);
 ClientRaw = trim(document.getElementById("client").value);
 
 // alert(DateRaw);
 // alert(TimeFromH);
 //  alert(TimeFromM);
 //   alert(TimeToH);
 //    alert(TimeToM);
 //    alert(id_issueRaw);
 //    alert(ClientRaw);
      
    
    if (TimeFromH == "none"){
    	TimeFrom = "none";
    }
    else if (TimeFromM == "none"){
    	TimeFrom = "none";
    }
    else {
    	TimeFrom = TimeFromH + TimeFromM;
    }
    
    if (TimeToH == "none"){
    	TimeTo = "none";
    }
    else if (TimeToM == "none"){
    	TimeTo = "none";
    }
    else {
    	TimeTo = TimeToH + TimeToM;
    }
        


  DateVal = if_empty (DateRaw, "overtime_f_jsExcl" );  
  TimeFromVal = if_empty (TimeFrom, "FromMsg" ); 
  TimeToVal = if_empty (TimeTo, "ToMsg" ); 
  ClientVal = if_empty(ClientRaw, "client_jsExcl");  
  id_issueVal = if_empty (id_issueRaw, "issue_Excl" );   
 


//   alert(DateVal);
//   alert(TimeFromVal);
//   alert(TimeToVal);
//   alert(ClientVal);
//   alert(id_issueVal);
      
      


if ( DateVal == "1C" )
  {
  	//thForm.dates.focus();
  	return false ; 	
  }
else if ( TimeFromVal == "1C" )
  {
  	return false ; 	
  }
else if ( TimeToVal == "1C" )
  {
  	return false ; 	
  }
else if ( ClientVal == "1C" )
  {
  	thForm.client.focus();
  	return false ; 	
  }  
else if ( id_issueVal == "1C" )
  {
  	thForm.issue.focus();
  	return false ; 	
  }      
    
}
function time_shown ( this_f, time_val, flag){
  //alert(this_f);
  if (flag == 'clear'){
    this_f.hours_h.disabled   = true;
    this_f.hours_m.disabled   = true;
    this_f.hour_from.disabled = true; 
    this_f.min_from.disabled  = true; 
    this_f.hour_to.disabled   = true; 
    this_f.min_to.disabled    = true; 
    
    this_f.hours_h.value   = 'none';
    this_f.hours_m.value   = 'none';
    this_f.hour_from.value = 'none'; 
    this_f.min_from.value  = 'none'; 
    this_f.hour_to.value   = 'none'; 
    this_f.min_to.value    = 'none'; 
  }
  else {
    if (time_val=='hours'){
      this_f.hour_from.disabled = true; 
      this_f.min_from.disabled  = true; 
      this_f.hour_to.disabled   = true; 
      this_f.min_to.disabled    = true; 
    }
    else if (time_val == 'from_to'){
      this_f.hours_h.disabled   = true;
      this_f.hours_m.disabled   = true;
    }
  }
  if (time_val == 'hours'){
    this_f.hours_h.disabled= false;
    this_f.hours_m.disabled= false;
  }
  else if (time_val == 'from_to'){
    this_f.hour_from.disabled = false; 
    this_f.min_from.disabled  = false; 
    this_f.hour_to.disabled   = false; 
    this_f.min_to.disabled    = false;
  }
}
function fit_users(thForm, RawStr , cases, name)
{
	//alert(RawStr);
	if(RawStr.value == 'ViSiToR'){
		//alert('test');
		inputText = "<label>Person<\/label><br /><input type=\"text\" class=\"input_assign\" name=\""+name+"\" >\n";
		document.getElementById('users').innerHTML = inputText;
	}
	else{
  //alert(cases);		
	RawStr=RawStr.value;
	RawStr = RawStr ? RawStr.split("~SePaRaToR_DEPT~") : '';
	Department = RawStr[0] ? RawStr[0] : '';
	//alert (Department);
	Users= RawStr[1] ? RawStr[1].split("~SePaRaToR~") : '';
	
	switch(cases)
  {
  case 'parking':
    Default_Option = "<option value=\"\">---<\/option>";
    break;
  case 'report':
    Default_Option = "<option value=\""+Department+"\">all<\/option>";
    break;
  default:
    Default_Option = "<option value=\""+cases+"\">"+cases+"<\/option>";
  }
  
  usersList = "<label>Person<\/label><br /><select name=\""+name+"\" >"+Default_Option+"\n";
	  for ( var i in Users ){
	  	
	  	  user = Users[i].split("~SePaRaToR_ID~");
        usersList = usersList+"<option value=\""+user[1]+"\">"+user[0]+"<\/option>\n";
         
    } 
    
   usersList = usersList+"<\/select>\n"; 
  //alert(usersList);
	document.getElementById('users').innerHTML = usersList;
  }
}
var btnWhichButton;

function park_alert_msg(FormName) 
{ 
		
  if (btnWhichButton){

	//alert(FormName);
	alert_msg = confirm("Are you sure you want to delete this record?"); 
 
   if (alert_msg == true){
   	 //alert(alert_msg);
   	  document.getElementById('delete').value = 'true';
     	return true;
   }
   else if(alert_msg == false){
   	// alert(alert_msg);
      document.getElementById('delete').value = 'false';
      document.getElementById('hidden_id').value = '';
      FormName.submit;
    }
  }
}
function popUp_3g(URL){
   //alert(URL);
   window.open(URL, '3g_details', 'width=900,height=550,left=200,top=100,scrollbars=no'); 
	
}

function popUp_3g_assing(URL){
   //alert(URL);
   window.open(URL, '3g_assign', 'width=460,height=340,left=500,top=100,scrollbars=no'); 
	
}var btnWhichButton;

function alert_msg_hotline() {
    if (btnWhichButton.name == 'delete_3g_card') {
        alert_msg = confirm("Do you realy want to delete the record for this card?");
        if (alert_msg == true) {
            return true;
        } else if (alert_msg == false) {
            document.getElementById('hidden_card_id').value = '';
            return false;
        }
    } else if (btnWhichButton.name == 'delete_laptop') {
        alert_msg = confirm("Do you realy want to delete the record for this card?");
        if (alert_msg == true) {
            return true;
        } else if (alert_msg == false) {
            return false;
        }
    }
}
var check_id = '';
var old_value;
function change_status_laptop(id, name){
	
	value = " <select style=\"height: 18px;\" name='"+name+"' onchange='document.laptop_form.submit();'>";
	value = value+"<option value='none'>- - -<\/option>";	
	value = value+"<option value='available'>available<\/option>";
	value = value+"<option value='broken'>broken<\/option>";
	value = value+"<option value='under repair'>under repair<\/option>";
	value = value+"<option value='Network management'>Network management<\/option>";
	value = value+"<option value='lost/stolen'>lost/stolen<\/option>"; //HRCENTER-1191
	value = value+"<\/select>";
	
	//get old status value
	get_old_status_value(id, check_id);
	
}

function change_status_keys(id, name){
	
	value = " <select style=\"height: 18px;\" name='"+name+"' onchange='document.laptop_form.submit();'>";
	value = value+"<option value='none'>- - -<\/option>";	
	value = value+"<option value='available'>available<\/option>";
	value = value+"<option value='broken'>broken<\/option>";
	value = value+"<option value='under repair'>under repair<\/option>";	
	value = value+"<\/select>";
	
	//get old status value
	get_old_status_value(id, check_id);
	
}

function change_status_3g(id, name){
	
	value = " <select style=\"height: 18px;\" name='"+name+"' onchange='this.form.submit();'>";
	value = value+"<option value='none'>- - -<\/option>";
	value = value+"<option value='available'>available<\/option>";
	value = value+"<option value='lost'>lost<\/option>";
	value = value+"<option value='active'>active<\/option>";
	value = value+"<option value='non-active'>non-active<\/option>";
	value = value+"<\/select>";
	
	//get old status value
	get_old_status_value(id, check_id);
	
}

function change_status_card_access(id, name){
	
	value = " <select style=\"height: 18px;\" name='"+name+"' onchange='document.card_access_form.submit();'>";
	value = value+"<option value='none'>- - -<\/option>";
	value = value+"<option value='available'>available<\/option>";		
	value = value+"<option value='to be reassign'>to be reassign<\/option>";	
	value = value+"<option value='out of use'>out of use<\/option>";
	value = value+"<option value='blocked'>blocked<\/option>";
	value = value+"<option value='lost'>lost<\/option>";
	value = value+"<option value='broken'>broken<\/option>";
	value = value+"<\/select>";
	
	//get old status value
	get_old_status_value(id, check_id);
	
}

//get old status value 
function get_old_status_value(id, check_id){
	if(id != ''){
		if(check_id == ''){
			check_id = id;
			old_value = document.getElementById(id).innerHTML;
			document.getElementById(id).innerHTML = value;
		}
		else if(check_id != id){
			document.getElementById(check_id).innerHTML = old_value;
			check_id = id;
			old_value = document.getElementById(id).innerHTML;
			document.getElementById(id).innerHTML = value;
		}
		else{
			document.getElementById(check_id).innerHTML = old_value;
			check_id = '';
		}
	}
}


function check_is_number(event){
	
	//pattern 0-9 and  . 
	var regex = /^[0-9]*\.?[0-9]*$/;
	
	//cathc the event key code for all browsers
    var get_event_char_code =(event.charCode) ? event.charCode : ((event.which) ? event.which : event.keyCode);  
    
    var get_event_char = String.fromCharCode(get_event_char_code);
   
    if(!get_event_char.match(regex)){ 
    	//return false does not work 
    	if (event.preventDefault) {
            event.preventDefault();
        } else {
            event.returnValue = false;
        }
    		
	}
}
function show_lunch(thForm){
  
 //alert(thForm.lunch_break.checked);
  if(thForm.lunch_break.checked == true){
    document.getElementById('lunch_div').style.display='';
  }
  else {
    document.getElementById('lunch_div').style.display='none';    
  }
}function ot_open_window(wurl, windowname, wwidth, wheight)
{ 
	if(!wwidth){wwidth = 1000;}
	if(!wheight){wheight = 700;}
  window.open(wurl, windowname, 'width='+wwidth+',height='+wheight+',left=100,top=100,scrollbars=1');
}var sec_options = new Array();

function disable_min (id_1, id_2, sec_array, form) {
    var select_box = document.getElementById(id_1);
    var changeField = document.getElementById(id_2);
    
    if (select_box.value == '24') {
        sec_options = document.getElementById(id_2).options;
        //alert(sec_options.length);    
        deleteOptions(document.getElementById(id_2)); // deleteOptions() is function from Meeting Rooms   
        newOption = new Option('00', '00');
        changeField.options[1] = newOption;    
    } else if (form) {
        if (form == 'AM' && select_box.value == '13') {
            sec_options = document.getElementById(id_2).options;
            deleteOptions(document.getElementById(id_2)); // deleteOptions() is function from Meeting Rooms   
            newOption = new Option('00', '00');
            changeField.options[1] = newOption;
        } else if (form == 'PM' && select_box.value == '18') {
            sec_options = document.getElementById(id_2).options;
            //alert(sec_options.length);    
            deleteOptions(document.getElementById(id_2)); // deleteOptions() is function from Meeting Rooms   
            newOption = new Option('00', '00');
            changeField.options[1] = newOption;
        } else if (form == 'wfh' && select_box.value == '18') {
            sec_options = document.getElementById(id_2).options;
            //alert(sec_options.length);    
            deleteOptions(document.getElementById(id_2)); // deleteOptions() is function from Meeting Rooms   
            newOption = new Option('00', '00');
            changeField.options[1] = newOption;
        }
    } else {
        sec_array = sec_array.split(',');
        deleteOptions(document.getElementById(id_2)); // deleteOptions() is function from Meeting Rooms   
        for ( var i in sec_array ) {
            j = changeField.options.length;
            newOption = new Option(sec_array[i], sec_array[i]);
            changeField.options[j] = newOption;
        }
    }
}function checkRequestedHours(){
  
  HFrom = document.getElementById('hour_from').value;
  MFrom = document.getElementById('sel_min_from').value;
  HTo   = document.getElementById('hour_to').value;
  MTo   = document.getElementById('sel_min_to').value;
  
  bal_val = document.getElementById('balance_value').innerHTML;
  bal_val = bal_val.replace("h", "");
  bal_val = bal_val.split(":");
  bal_min = (bal_val[0]*60)+(bal_val[1]*1);
  
  from_min = (HFrom*60)+(MFrom*1);
  to_min   = (HTo*60)+(MTo*1);
  
  var lunch_break = getLunchBreakTime();
  req_val  = (to_min - from_min) - lunch_break;
  
  if(isNaN(req_val)){
    alert('Please select hours');
    return false;
  }
  else if((bal_min - req_val) < 0){
    return confirm("You are about to insert record more then the requested hours. \n Are you sure?");
  }
}

function getLunchBreakTime(){

	  var lunch_from = 0;
	  var lunch_to = 0;
	  var calculate_lunch_brake = 0;

	  
	      if((document.getElementById('lunch_break').value == null) 
	    		  || (document.getElementById('lt_hour_from').value == null) 
	    		  || (document.getElementById('sel_lt_min_from').value == null) 
	    		  || (document.getElementById('lt_hour_to').value == null)
	    		  || (document.getElementById('sel_lt_min_to').value == null) ){
	 		 
	 	   }else{
	 		    lunch_from = document.getElementById('lt_hour_from').value * 60 + document.getElementById('sel_lt_min_from').value * 1;
	 		    lunch_to = document.getElementById('lt_hour_to').value * 60 + document.getElementById('sel_lt_min_to').value * 1;
	 		    
	 		    var lunch_from_sum = isNaN(lunch_from) ? 0 : lunch_from;
	 		    var lunch_to_sum = isNaN(lunch_to) ? 0 : lunch_to;	
	 		    calculate_lunch_brake = lunch_to_sum - lunch_from_sum;	
	 		    
	 	   }
	      
	      return calculate_lunch_brake;
	  
}function checkIsOldDateTime(flag, record_class){	
	if(flag == 'false' && record_class == ''){
		alert('You inserting new meeting for old date or old hour. ');
		return 'false';
	}
	
	return 'true';
	
}

function checkRoom(room_id,position, select_date){
	var current_date  = new Date(select_date);
	var start_date_save_room = new Date('2016-04-12');	
	//check for room 6 with rom_id=9 - may be to september 2016 use
	 if ( room_id==9 ){
		 alert('This room is reserved for IMX v9 meetings!');
		 result ="No";
		 return result;
	 }
	 
	 
 }

function check(room_id,position, userId){
	 
	 if (room_id==11){
		 if ((position!=='Administration') && (position!=='HR') && (userId !== 990) && (userId !== 1182)){
			 alert('If you need to book the room for certain period, please turn HR to do the booking for you!');
			 result ="No";
			 return result;
		 }
	 }
	 
	 if (room_id==10 && (userId !== 920 && userId !== 1184 && userId !== 1250 && userId !== 1340 && userId !== 1361
                         && userId !== 1399 && userId !== 1507 && userId !== 1745 && userId !== 2131)){
		     alert('The room BARCELONA 12-R-1 is dedicated to training sessions and cannot be booked directly.\n' +
				 'To request a booking for other purposes, ensure it is free for the desired time slot ' +
				 'and email <trainings_organization@codixgroup.onmicrosoft.com> for approval and reservation.')
			 result ="No";
			 return result;
		 
	 }
	 //if ((room_id==3)){
		 //alert('This room is forbidden for reservation till end of year!');
		 //result ="No";
		// return result;
	 //}
 }
 function createOverlib(time,day_date,room_id,rooms){
	//alert(room_id);
	
	 if (window.XMLHttpRequest){
    // code for IE7+, Firefox, Chrome, Opera, Safari
    xmlhttp=new XMLHttpRequest();
   }
   else{
   	// code for IE6, IE5
    xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
   }
   
   xmlhttp.onreadystatechange=function(){
     //alert(xmlhttp.responseText);
	  
     if (xmlhttp.readyState==4 && xmlhttp.status==200){    	
       overlib_text = xmlhttp.responseText.split('~<>~');
       overlib(overlib_text[1],FADEIN , '0',FADEOUT ,'0', TEXTSIZE, '10px', FGCOLOR, '#FFFFEE',BGCOLOR, '#CCCCAA',STICKY, CLOSETEXT, '', CAPTION, overlib_text[0], DRAGGABLE, DRAGCAP);
       var get_cal_btn2 = document.getElementById("cal_btn2");
       var cal_btn2 = get_cal_btn2 ? "cal_btn2":"";
       Calendar.setup({
            trigger    : cal_btn2,
            inputField : "showDate2",
            onSelect   : function() { 
    	         //loop users data
    	   		 usersInfo();                 
                 this.hide(); 
             }
        });
     }
   } 
  
   
   xmlhttp.open("POST", "meeting_rooms/createOverlib.php", true);
   xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
   xmlhttp.send("time="+time+"&date="+day_date+"&room_id="+room_id+"&rooms="+rooms);
	
 }
 
 function checkUserInLeave(userId, date, index, callback){ 	
	 	
		var allIdUsers = userId; 		 		 	
	 	
	 	if (window.XMLHttpRequest){
	     	// code for IE7+, Firefox, Chrome, Opera, Safari
	      xmlhttp=new XMLHttpRequest();
	    }
	    else{
	     	// code for IE6, IE5
	      xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
	    }
	 	
	 	  xmlhttp.onreadystatechange=function()
	 	  {
	 		
	 	    if (xmlhttp.readyState==4 && xmlhttp.status==200){ 	
	 	    	//leave user info	    	
	 	    	getUserLeaveId = xmlhttp.responseText;
	 	    	
	 	    	//clear lists from users when change date for meeting
	 	    	if(index==0){
	 	    		document.getElementById("invated_users").innerHTML = "";
	 	    	     document.getElementById('invated_users').value = "";
	 	    	}
	 	    	
	 	    	//check if have leave records for users
	 	    	if(getUserLeaveId){
	 	    		//split all user ids
		 	    	getUserLeaveId = getUserLeaveId.split(';');
					//style color css
					spanColor = getUserLeaveId[1]+ ' ' + getUserLeaveId[2];
		 	    	
					//leave record
					leaveType = "<span class=\"" + spanColor + "\">("+getUserLeaveId[0]+")</span>";	
					
					//get conditions for leave records else no anything leave records
					if(getUserLeaveId[6]){				
						document.getElementById('invated_users').innerHTML = document.getElementById('invated_users').innerHTML +
						  "<div class=invated_user id=user"+getUserLeaveId[1]+" ><span style=float:left; >"+getUserLeaveId[4]+ ' ' + getUserLeaveId[5]+"<span><\/span>&nbsp;&nbsp;("+getUserLeaveId[6]+")&nbsp;&nbsp;<\/span> "+leaveType+"<input type=button onclick=removeFromListOfUsers('"+getUserLeaveId[1]+"'); ><\/div>"; 
					}else{							
		 	    		document.getElementById('invated_users').innerHTML = document.getElementById('invated_users').innerHTML +
						  "<div class=invated_user id=user"+getUserLeaveId[0]+" ><span style=float:left; >"+getUserLeaveId[1]+ ' ' + getUserLeaveId[2]+"<span><\/span>&nbsp;&nbsp;("+getUserLeaveId[3]+")&nbsp;&nbsp;<\/span><input type=button onclick=removeFromListOfUsers('"+getUserLeaveId[0]+"'); ><\/div>"; 
			 	    
					}
					
	 	    	}
	 	    	
	 	    }
	 	  }  
	 	 //get hour (from)
	 	 var from = document.getElementById('from');
		 var strFrom = from.options[from.selectedIndex].value;
		  
		//get hour (to)
		 var to = document.getElementById('to');
		 var strTo = to.options[to.selectedIndex].value;
	 	
	 	// Third value can be true or false. True = asynchronous False = Synchronous
	    xmlhttp.open("POST", "src/modules/meetingrooms/ajax/leaveUsers.php", false);
	    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	    
	    xmlhttp.send("userId=" + allIdUsers + "&eventDate=" + date+ "&strFrom=" + strFrom + "&strTo=" + strTo);
	    
	}
 
 function usersInfo(){
   //get each of user id from lists of users
   userId = document.getElementById('hidden_users_id').value;
   date = document.getElementById('showDate2').value;
  
   //add element with concatenate -;- to the other elements (all id users)
   userId = userId.replace("--", "-;-");
   users = userId.split(';');    	  
   //loop users id
   for(i in users){
  		
  		if(!users[i]){
  			continue;
  		}    	  		
  		
  		checkUserInLeave(users[i], date, i);    	  		
  	}		    	   
 }
 
 function countChars(field1, numb, field2){
 	
 	 var str = field1.value;
 	
 	 if(str.length > numb){
 		 field1.value = str.substring(0,numb);
   }
 	 else{
 	   var available_chars = numb - str.length;
 	   document.getElementById(field2).innerHTML = available_chars;
   }
  
 }
 
 function recalculateTime(field1, field2, time_str, diff){
   var time_arr = time_str.split(',');

 	 var time = field1.options[field1.selectedIndex].value;
 	 var time2 = field2.options[field2.selectedIndex].value;

   deleteOptions(field2);
 	 field2.remove(0);

 	 for(var i in time_arr){

 	 	 if(field1.name == 'from' && time_arr[i]*1 <= time*1){
 	 	 	 continue;
 	 	 }
 	 	 else if(field1.name == 'to' && time_arr[i]*1 >= time*1){
 	 	   //continue;
 	 	 }

 	 	 var newOption = new Option(m2h(time_arr[i]), time_arr[i]);
 	 	 j = field2.options.length;
     field2.options[j] = newOption;

 	 	 if(time_arr[i]*1 == time2*1 && diff*1 == 0){
 	 	 	 field2.options[j].selected = true;
 	 	 }
     else if(diff*1 != 0 && time_arr[i]*1 == (time*1 + diff*1)){
       field2.options[j].selected = true;
     }

 	 }

 }
 
 function m2h(min){
 	
   var hours = Math.floor(min/60);
   var minutes = min%60;
   
   if(minutes == 0){
   	 minutes = '00';
   }
   
   var time = hours + ":" + minutes;
   return time;
   
 }
 
 function fromHour(){	
	 usersInfo();
 }
 
 function toHour(){
	 usersInfo();
 }
 function removeFromListOfUsers(user_id){
	  if(confirm('Are you sure you want to remove this person?')){
		  hidden_users_id = document.getElementById('hidden_users_id').value;
		  document.getElementById('hidden_users_id').value = hidden_users_id.replace(user_id+";","");
		  document.getElementById('invated_users').removeChild(document.getElementById('user'+user_id));
	  }
	  
}
function fitUsers(changeField, sel_value){

 	if (window.XMLHttpRequest){
     	// code for IE7+, Firefox, Chrome, Opera, Safari
      xmlhttp=new XMLHttpRequest();
    }
    else{
     	// code for IE6, IE5
      xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
    }
       
    xmlhttp.onreadystatechange=function(){
      if (xmlhttp.readyState==4 && xmlhttp.status==200){
      	//alert(xmlhttp.responseText);
      	newOptions = xmlhttp.responseText.split('\n');
      	for( var i in newOptions ){
      		if(!newOptions[i]){ continue;}
      		newOption = newOptions[i].split('~');
      		newOption = new Option(newOption[1], newOption[0]);
      		j = changeField.options.length;
      		changeField.options[j] = newOption;
      	}

      }
    }
 
    xmlhttp.open("POST", "meeting_rooms/searchUsers.php", true);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.send("sel_value="+sel_value);
    
}
 

function deleteOptions(changeField){
 	
   for(opt = changeField.options.length-1; opt > 0; opt--){
     changeField.remove(opt);
   }
    
}

function checkIfUserInLeave(userId, date, callback){ 	
 	
	var allIdUsers = userId; 	
 	var returnValue;
 	
 	if (window.XMLHttpRequest){
     	// code for IE7+, Firefox, Chrome, Opera, Safari
      xmlhttp=new XMLHttpRequest();
    }
    else{
     	// code for IE6, IE5
      xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
    }
 	
 	  xmlhttp.onreadystatechange=function()
 	  {
 		
 	    if (xmlhttp.readyState==4 && xmlhttp.status==200){ 	
 	    	//alert(xmlhttp.responseText); 	 	    	
 	    	getUserLeaveId = xmlhttp.responseText; 	    	 
 	    }
 	  }  
 	  
 	 var from = document.getElementById('from');
	 var strFrom = from.options[from.selectedIndex].value;
	  
	 var to = document.getElementById('to');
	 var strTo = to.options[to.selectedIndex].value;
 	
 	// Third value can be true or false. True = asynchronous False = Synchronous
    xmlhttp.open("POST", "src/modules/meetingrooms/ajax/leaveUsers.php", false);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    
    xmlhttp.send("userId=" + allIdUsers + "&eventDate=" + date+ "&strFrom=" + strFrom + "&strTo=" + strTo);
    
}

function moveUser(user, department, getEventDateObject, from, to){
	
	var getEventDate = getEventDateObject.value; 
	user_id = user.options[user.selectedIndex].value;
	user_name = user.options[user.selectedIndex].text;
	hidden_users_id = document.getElementById('hidden_users_id').value;	
	
  if(user_name == 'all'){
  	users = user_id.split(';');
  	for(i in users){
  		
  		if(!users[i]){
  			continue;
  		}
  		
  		user_data = users[i].split('!');  		
  		chanUserData(user_data, getEventDate);
  	}
  	
  }
  else if(user_id != 'none'){

	  user_id = user_name+"^"+user_id;

	  user_data = user_id.split('^');

  	if(hidden_users_id.search(user_data[0]) == -1){
  		chanUserData(user_data, getEventDate);
    }
    else{
  	  alert('Already in the list');
    }
	}
	
}


function chanUserData ( user_data, getEventDate ){
	var spanColor;
	var leaveType = '';

	if(hidden_users_id.search(user_data[1]) == -1){
			hidden_users_id = document.getElementById('hidden_users_id').value;
			checkIfUserInLeave(user_data[1], getEventDate);	
			getUserLeaveId = getUserLeaveId.replace(/^\s+|\s+$/gm,'');
			
			getUserLeaveId = getUserLeaveId.split(';');
			
			spanColor = getUserLeaveId[1]+ ' ' + getUserLeaveId[2];
			
			//spanColor = "sick_leave  tl_halfday_FULL4";
			//check getUserLeaveId[0] is not number
			if ( isNaN(getUserLeaveId[0]) ){	
				leaveType = "<span class=\"" + spanColor + "\">("+getUserLeaveId[0]+")</span>";
			}			
			
			
      document.getElementById('hidden_users_id').value = hidden_users_id+user_data[1]+";";      
      document.getElementById('invated_users').innerHTML = document.getElementById('invated_users').innerHTML+"<div class=invated_user id=user"+user_data[1]+" ><span style=float:left; ><span>"+user_data[0]+"<\/span>&nbsp;&nbsp;("+user_data[2]+")&nbsp;&nbsp;<\/span> "+leaveType+"<input type=button onclick=removeUser('"+user_data[1]+"'); ><\/div>";        
  }
}

function removeUser(user_id){
  if(confirm('Are you sure you want to remove this person?')){
	  hidden_users_id = document.getElementById('hidden_users_id').value;
	  document.getElementById('hidden_users_id').value = hidden_users_id.replace(user_id+";","");
	  document.getElementById('invated_users').removeChild(document.getElementById('user'+user_id));
  }
  
}function highlight(room, time){
    if(document.getElementById(room) != null){
	  document.getElementById(room).style.backgroundColor  = "#CCD0D4";
    }
    
    if(document.getElementById(time) != null){
	  document.getElementById(time).style.backgroundColor  = "#CCD0D4";
    }
	
}

function unhighlight(room, time){
	if(document.getElementById(room) != null){
	  document.getElementById(room).style.backgroundColor  = "#EEF2F6";
	}
    if(document.getElementById(time) != null){
	 document.getElementById(time).style.backgroundColor  = "#FFFFFF";
    }
	
}

function unhighlight2(room, time){

	if(document.getElementById(room) != null){
	  document.getElementById(room).style.backgroundColor  = "#FFFFFF";
	}
	if(document.getElementById(time) != null){
	  document.getElementById(time).style.backgroundColor  = "#FFFFFF";
	}
	
}function checkReservationForm(form, btn){
  
   from      = form.from.value; 
	 to        = form.to.value; 
	 date_r    = form.date.value;
	 room_id   = form.room_id.value;
	 record_id = form.record_id.value;
	 desc      = form.description.value;
	 subject   = form.subject.value;
	 if(form.Projector.checked == true){
	   projector = 1;
	 }
	 else{
	 	 projector = 0;
	 }

   
   if(btn.name == 'delete'){
   	if(confirm('Are you sure you want to delete this reservation?')){
   	  form.hidden_action.value = btn.name;
      form.submit();
    }
   }
   else if(subject == ''){
	   alert('Please fill subject');
	 }
	 else if(desc == ''){
	   alert('Please fill description');
	 }
	 else{
	 	
     if (window.XMLHttpRequest){
     	// code for IE7+, Firefox, Chrome, Opera, Safari
      xmlhttp=new XMLHttpRequest();
     }
     else{
     	// code for IE6, IE5
      xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
     }
     
     xmlhttp.onreadystatechange=function(){
     	  //alert(xmlhttp.responseText);
       if (xmlhttp.readyState==4 && xmlhttp.status==200){
       
         if(xmlhttp.responseText == "false"){
         	 alert("You insert/update meeting for old date or old hour.");
         }
         else if(xmlhttp.responseText == "projector"){
        	 alert("More then available projector requested");
         }
         else if(xmlhttp.responseText == "overlapping"){
           alert('Overlaps with another reservation');
         }
         else{
         	 form.hidden_action.value = btn.name;
           form.submit();
         }
       }
     }
     
     //xmlhttp.open("POST", "meeting_rooms/check_records.php", true);
     xmlhttp.open("POST", "index.php?ajax=check_meeting_records", true);
     xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
     xmlhttp.send("from="+from+"&to="+to+"&date="+date_r+"&room_id="+room_id+"&record_id="+record_id+"&projector="+projector+"&action="+btn.name);
   
   }
 	 	
 	 
}function lv_form_actions(action_name){  
  xmlhttp = createXMLHttp();
  
  xmlhttp.onreadystatechange=function(){
    //alert (xmlhttp.responseText);
    if (xmlhttp.readyState==4 && xmlhttp.status==200){ 	       
      document.getElementById("this_id_changes").innerHTML = xmlhttp.responseText;
    }
  }
  
  if(action_name != ''){
    action_vars = get_action_vars (action_name);
  }
  
  xmlhttp.open("POST", 'leaves/ajax/lv_form_actions.php', true);
  xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
  xmlhttp.send(action_vars);
}

function get_action_vars(action_name){
  now = new Date;
  var yearNow  = now.getFullYear();
  var dateNow  = now.getDate()+'';
  if (dateNow.length == 1){
    dateNow = "0"+dateNow;
  }
  var monthNow = (now.getMonth()+1+'');
  if (monthNow.length == 1){
    monthNow = '0'+monthNow;
  }
  
  var today = yearNow+''+monthNow+''+dateNow;
 
  switch (action_name){
    case "select_lv_group":
      var select_lv_group = document.getElementById('select_lv_group').value;
      return("&select_lv_group="+select_lv_group);
      break;
    case "select_lv_type":
      var select_lv_group = document.getElementById('select_lv_group').value;
      var select_lv_type  = document.getElementById('select_lv_type').value;
      var select_lv_type_text = document.getElementById('select_lv_type');
      select_lv_type_text = select_lv_type_text.options[select_lv_type_text.selectedIndex].text;
      var selected_dates = cal.selection.get();

      while (selected_dates.length > 0){
        if (isArray(selected_dates[0]) == true){
          while (selected_dates[0].length > 0){
            cal.selection.unselect(selected_dates[0][0]);
          }
        }
        else {
          cal.selection.unselect(selected_dates[0]); 
        }           
      }
                  
      if (select_lv_type_text == 'planned'){
        allow_dt(today, '', 'not_null');
      }
      else if (select_lv_type_text == 'emergency'){
        allow_dt('', today, 'not_null');        
      }
      else {
        allow_dt('', '', 'not_null');
      }      
      return("&select_lv_group="+select_lv_group+"&select_lv_type="+select_lv_type);
      break;
  }
}
  
  function loadLeaveTypes(changeField, leave_group){
    
    deleteOptions(changeField);
    
    xmlhttp = createXMLHttp();
       
    xmlhttp.onreadystatechange=function(){
    	//alert(xmlhttp.responseText);
      if (xmlhttp.readyState==4 && xmlhttp.status==200){

      	newOptions = JSON.parse(xmlhttp.responseText);
      
      	for( var i in newOptions ){
      		newOption = newOptions[i];
      		newOption = new Option(newOption[1], newOption[0]);
      		j = changeField.options.length;
      		changeField.options[j] = newOption;
      	}

      }
    }
 
    xmlhttp.open("POST", "leaves/ajax/loadLeaveTypes.php", true);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.send("leave_group="+leave_group);
    
 }
 
 function loadLeaveForm(type_id, field){
 	 
 	 xmlhttp = createXMLHttp();
       
    xmlhttp.onreadystatechange=function(){
    	if (xmlhttp.readyState==4 && xmlhttp.status==200){
        //alert(xmlhttp.responseText);
      	document.getElementById(field).innerHTML = xmlhttp.responseText;
      }
    }
 
    xmlhttp.open("POST", "../ajax/loadLeaveForm.php", true);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.send("type_id="+type_id);
 }
 
 function deleteOptions(changeField){

 	  for(opt = changeField.options.length-1; opt > 0; opt--){
      changeField.remove(opt);
    }
    
 }
 
 var multi_dept = false;
 
 function loadUsers(changeField, dept){
    
    deleteOptions(changeField);
    if(multi_dept != false){
    	
    	var count_selected = 0;
    	
    	for(opt = multi_dept.options.length-1; opt > 0; opt--){
        if(multi_dept.options[opt].selected == true){
        	count_selected++;
        }
      }
    	
    	if(count_selected > 1){
    		//newOption = new Option('all', 'all');
      	//changeField.options[1] = newOption;
      	return;
    	}
    	
    }
    
    xmlhttp = createXMLHttp();
       
    xmlhttp.onreadystatechange=function(){
    	
      if (xmlhttp.readyState==4 && xmlhttp.status==200){
        //alert(xmlhttp.responseText);
      	newOptions = JSON.parse(xmlhttp.responseText);
        
        for(var i=0; i < newOptions.length; i++ ){      	  
      		newOption = newOptions[i];
      		newOption = new Option(newOption[1], newOption[0]);
      		j = changeField.options.length;
      		changeField.options[j] = newOption;
      	}

      }
    }
 
    xmlhttp.open("POST", "leaves/ajax/loadUsers.php", true);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.send("dept="+dept);
    
 }
  
 function move_selected(){
	
	var newOption;
	var j;
	var selFild = document.getElementById('select_users');
	var selectedFild = document.getElementById('selected_users');
	 
	 for (i=0; i<selFild.options.length; i++) {
  	 	 
  	 	 
  	 	 if(selFild.options[i].selected){
  	 	    
  	 	    for(var e=0; e< selectedFild.length; e++){
        		 if(selectedFild.options[e].value == selFild.options[i].value){
        		   selectedFild.remove(e);        		   
        		   return;
        		 }
        	 }
  	 	    if (selFild.options[i].value != 'all'){
    	 	 	  newOption = new Option(selFild.options[i].text,selFild.options[i].value);
    	 	 	  if(selectedFild.options.length !=0){
    	 	 	   j=selectedFild.options.length;
    	 	 	  }
    	 	 	  else{
    	 	 	  	j=0;
    	 	 	  }
    	 	
            selectedFild.options[j] = newOption;
          }
           
  	   }
  	   
     }
     var e;          
   
  }
  
function move_selected_back(){
	
	var newOption;
	var j;
	var selFild = document.getElementById('select_users');
	var selectedFild = document.getElementById('selected_users');
	
   var e;
   for(e=selectedFild.options.length-1;e>=0;e--){
   	
   if(selectedFild.options[e].selected)
     selectedFild.remove(e);
   }
 
}

function select_filter_users (){
  var selectedFild = document.getElementById('selected_users');
  for (i=0; i<selectedFild.options.length; i++) {
    selectedFild.options[i].selected = 'selected';
  }
}

  /* this function fill selected date
 	 * from calendar in text fields
 	 */
  function updateFields(cal){

		var sel_dates = cal.selection.print("%d %b %Y", " - "); 
    if (document.getElementById("show_selected_dates") && sel_dates) {
    	sel_dates = sel_dates+'';
      sel_dates = sel_dates.replace(/,/g, '\n');
      document.getElementById("show_selected_dates").value = sel_dates;            
    }
    
    var sel_dates = cal.selection.print("%Y-%m-%d", ">");
    //alert(document.getElementById("selected_dates"));
    document.getElementById("selected_dates").value = printSelection(cal);
    
  };
  
  function printSelection(cal) {
    var sel = cal.selection.print("%Y%m%d", ", ");
    var selection = '';
    
    for(var i in sel){
    	
    	if(sel[i].search(',') == -1){
    		if(selection == ''){
    			selection = sel[i];
    		}else{
    	    selection = selection + ', ' + sel[i];
    	  }
      }else{
      	if(selection == ''){
    			selection = '['+sel[i]+']';
    		}else{
      	  selection = selection + ', ' + '['+sel[i]+']';
        }
      }
    	
    }
    
    return '[' + selection + ']';
  
  };
  
  /* this function change date style
 	 * if there is record about it in the DATE_INFO object
 	 */
  function DateInfo(date, wantsClassName){
				
	  var as_number = Calendar.dateToInt(date);
			 
		return DATE_INFO[as_number];
		
	};
  
  /* this function select user data from database
 	 * and create DATE_INFO object which is then send to calendar
 	 */
 	
 	function getDateInfo(cal){

    var month = cal.date.getMonth()+1;
    if(month < 10){
    	month = '0'+month;
    }
    //month = '__'; //select date for all year
    var year  = cal.date.getFullYear();

    xmlhttp = createXMLHttp();
    
    xmlhttp.onreadystatechange=function(){
      //alert (xmlhttp.responseText);
      if (xmlhttp.readyState==4 && xmlhttp.status==200){
      	//alert(xmlhttp.responseText);
      	if(xmlhttp.responseText != 'null'){
      	  DATE_INFO = JSON.parse(xmlhttp.responseText);
          cal.redraw();
        }
        
      }
    }
    
    xmlhttp.open("POST", 'leaves/ajax/getDateInfo.php', true);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.send("user_id="+user_id+"&year="+year+"&month="+month);

  };
 	
 	
 	/* this function checks if object is empty 
 	 * false = not empty/ true = empty
 	 */
	function isEmpty(ob){
    for(var i in ob){ return false;}
    return true;
  }
   
   /* this function disables dates in calendar
 	  *
 	  */
   function allow_dt (minimum, maximum, null_flag){
      var date1 = Calendar.intToDate(minimum);
      var date2 = Calendar.intToDate(maximum);              
      cal.args.min = date1;
      cal.args.max = date2;
      cal.redraw();
      if (null_flag == 'null'){
        document.getElementById("input_id").value = '';
        document.getElementById("emergency_date").value = '';
      }
   };
function hide_rep_form(elem_id, show_elem_id){  
  elem_arr = document.getElementById(elem_id).getElementsByTagName('*');
  for (var i=0; i < elem_arr.length; i++){
    var elem_name = elem_arr[i].name;
    //alert(elem_name);
    elem_arr[i].disabled = true;
  }
  
  elem_arr_show = document.getElementById(show_elem_id).getElementsByTagName('*');
  for (var i=0; i < elem_arr_show.length; i++){
    var elem_name_show = elem_arr_show[i].name;
    //alert(elem_name);
    elem_arr_show[i].disabled = false;
  }
}let minHeight = 600; //HRCENTER-1391
function create_UI_default(doc_name, ui_title, variables) {
	let div = document.createElement("div");
	div.setAttribute("id", "dialog-modal");
	let iframe = document.createElement("iframe");
	iframe.setAttribute("id", "ui_iframe");
	iframe.setAttribute("onload", "document.getElementById('ui_iframe').style = 'background: none; width: auto; min-height: "+minHeight+"px; height: auto;'"); //HRCENTER-1391  //style.background = 'none'
	iframe.setAttribute("src", "leaves/add/loadUIfile.php?ui_file="+doc_name+variables);
	iframe.setAttribute("frameBorder", "0");
	let laod_div = document.createElement("div");
	laod_div.setAttribute("id", "lv_load_ui");

	div.appendChild(iframe);
	document.body.appendChild(div);

	uiDialogOverlay(ui_title, 'auto', 'auto');
}

function create_UI(doc_name, lv_date, lv_user_id, day_type_id, level, ui_title, lv_id, two_halfs_lv_ids,halfday_value) {
	Cookies.set('save_scroll', getPageScroll());

	$('.ui-dialog').css('min-height: 600px;');
	let div = document.createElement("div");
	div.setAttribute("id", "dialog-modal");
	let iframe = document.createElement("iframe");
	iframe.setAttribute("id", "ui_iframe");
	iframe.setAttribute("onload", "document.getElementById('ui_iframe').style = 'background: none; width: auto; min-height: "+minHeight+"px; height: auto;'");  //HRCENTER-1391  //style.background = 'none'
	iframe.setAttribute("src", "leaves/add/loadUIfile.php?ui_file="+doc_name+"&lv_date="+lv_date+"&lv_user_id="+lv_user_id+"&day_type_id="+day_type_id+"&level="+level+"&lv_id="+lv_id+"&two_halfs_lv_ids="+two_halfs_lv_ids+"&halfday_value="+halfday_value);
	iframe.setAttribute("frameBorder", "0");
	let laod_div = document.createElement("div");
	laod_div.setAttribute("id", "lv_load_ui");

	div.appendChild(iframe);
	document.body.appendChild(div);

	uiDialogOverlay('Leave record: '+ui_title, 'auto', 'auto', 'overlay_showmap');
}

function uiDialogOverlay(title, width, height, dialogClass) {
	$("#dialog-modal" ).dialog({
		modal: true,
		resizable: false,
		draggable: true,
		dialogClass: dialogClass,
		minHeight: minHeight,    //HRCENTER-1391
		width: width,
		height: height,
		title: title,
		position: {
			my: "center top",
				at: "center top+20",
				of: window
		},
		close: function(event, ui) {
			//document.getElementById('leave_form').submit();
			$(this).remove();
			return false;
		}
	});

    // HRCENTER-1391
	$('#dialog-modal').css('min-height', minHeight + 'px');
	$( ".ui-widget-overlay" ).bind('click', function(event, ui) {
		$( "#dialog-modal" ).dialog("close");
	});
}

function create_UI_usettings(doc_name, ui_title, variables) {
	let div = document.createElement("div");
	div.setAttribute("id", "dialog-modal");
	let iframe = document.createElement("iframe");
	iframe.setAttribute("id", "ui_iframe");
	iframe.setAttribute("onload", "document.getElementById('ui_iframe').style = 'background: none; width: auto; min-height: "+minHeight+"px; height: auto;'"); //HRCENTER-1391  //style.background = 'none'
	iframe.setAttribute("src", "extra_hours/add/admin/loadUI_usettings.php?ui_file="+doc_name+variables);
	iframe.setAttribute("frameBorder", "0");
	let laod_div = document.createElement("div");
	laod_div.setAttribute("id", "lv_load_ui");

	div.appendChild(iframe);
	document.body.appendChild(div);

	uiDialogOverlay(ui_title, 'auto', 'auto');
}

function create_UI_form(name, ui_title, variables) {
	let div = document.createElement("div");
	div.setAttribute("id", "dialog-modal");
	let iframe = document.createElement("iframe");
	iframe.setAttribute("id", "ui_iframe");
	iframe.setAttribute("onload", "document.getElementById('ui_iframe').style = 'background: none; width: auto; min-height: "+minHeight+"px; height: auto;'"); //HRCENTER-1391  //style.background = 'none'
	iframe.setAttribute("src", "ajax/load_ui.php?ui_file="+name+variables);
	iframe.setAttribute("frameBorder", "0");
	let laod_div = document.createElement("div");
	laod_div.setAttribute("id", "lv_load_ui");

	div.appendChild(iframe);
	document.body.appendChild(div);

	uiDialogOverlay(ui_title, 'auto', 'auto');
}

function change_dates_in_insert_leave_form(start_date_val, end_date_val, lv_user_id, level, leave_type) {
	let xmlhttp;
	if (window.XMLHttpRequest) {
		// code for IE7+, Firefox, Chrome, Opera, Safari
		xmlhttp = new XMLHttpRequest();
	} else {
		// code for IE6, IE5
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	}

	xmlhttp.onreadystatechange=function() {
		if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
			let str_json = xmlhttp.responseText;
			let json = jQuery.parseJSON(str_json);

			if (json["success"]) {
				let type_lv_id = json['TYPE_LV_ID'];
				let halfday = json['HALFDAY'];
				let lv_id = json['LV_ID'];
				let lv_date = json['lv_date'];
				let two_halfs_lv_ids = json['two_halfs_lv_ids'];
				parent.frames[0].location = "../add/loadUIfile.php?ui_file=workday_ui&level="+level+"&two_halfs_lv_ids="+two_halfs_lv_ids+"&leave_type="+leave_type+"&lv_date="+lv_date+"&day_type_id="+type_lv_id+"&lv_id="+lv_id+"&halfday_value="+halfday+"&lv_start_dt="+start_date_val+"&lv_end_dt="+end_date_val+"&lv_user_id="+lv_user_id;
			}
		}
	}

	xmlhttp.open("POST", "../../index.php", true);
	xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	// xmlhttp.send("lv_start_dt="+lv_start_dt+"&lv_end_dt="+lv_end_dt);
	xmlhttp.send("lv_start_dt="+start_date_val+"&lv_end_dt="+end_date_val+"&lv_user_id="+lv_user_id);
}
