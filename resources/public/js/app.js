if(typeof Math.imul == "undefined" || (Math.imul(0xffffffff,5) == 0)) {
    Math.imul = function (a, b) {
        var ah  = (a >>> 16) & 0xffff;
        var al = a & 0xffff;
        var bh  = (b >>> 16) & 0xffff;
        var bl = b & 0xffff;
        // the shift by 0 fixes the sign on the high part
        // the final |0 converts the unsigned value into a signed value
        return ((al * bl) + (((ah * bl + al * bh) << 16) >>> 0)|0);
    }
}


;(function(){
/*

 Copyright The Closure Library Authors.
 SPDX-License-Identifier: Apache-2.0
*/
            ;var b=this||self;function e(a){const c=a.length;if(0<c){const h=Array(c);for(let d=0;d<c;d++)h[d]=a[d];return h}return[]};var f={},g={};if("undefined"===typeof f||"undefined"===typeof g||"undefined"===typeof k)var k={};if("undefined"===typeof f||"undefined"===typeof g||"undefined"===typeof l)var l=null;if("undefined"===typeof f||"undefined"===typeof g||"undefined"===typeof m)var m=null;if("undefined"===typeof f||"undefined"===typeof g||"undefined"===typeof n)var n=null;
var p="undefined"!==typeof Math&&"undefined"!==typeof Math.imul?function(a,c){return Math.imul(a,c)}:function(a,c){var h=a&65535,d=c&65535;return h*d+((a>>>16&65535)*d+h*(c>>>16&65535)<<16>>>0)|0};function q(a){a=p(a|0,-862048943);a=0^(p(a<<15|a>>>-15,461845907)|0);a=(p(a<<13|a>>>-13,5)+-430675100|0)^0;a=p(a^a>>>16,-2048144789);p(a^a>>>13,-1028477387)}q(1);q(0);if("undefined"===typeof f||"undefined"===typeof g||"undefined"===typeof r)var r=null;
"undefined"!==typeof console&&(l=function(){var a=arguments;return console.log.apply(console,e.call(null,a))},m=function(){var a=arguments;return console.error.apply(console,e.call(null,a))});if("undefined"===typeof f||"undefined"===typeof g||"undefined"===typeof t)var t=function(){throw Error("cljs.core/*eval* not bound");};function u(){var a=document.getElementById("app");return null!=a&&!1!==a?a.innerHTML="\x3ch1\x3eVolis Challenge\x3c/h1\x3e\x3cp\x3eFrontend ClojureScript inicial.\x3c/p\x3e":null}var v=["volis_challenge","ui","core","init"],w=b;v[0]in w||"undefined"==typeof w.execScript||w.execScript("var "+v[0]);for(var x;v.length&&(x=v.shift());)v.length||void 0===u?w=w[x]&&w[x]!==Object.prototype[x]?w[x]:w[x]={}:w[x]=u;
})();
