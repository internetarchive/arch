function t(t,e,i,s){var o,r=arguments.length,n=r<3?e:null===s?s=Object.getOwnPropertyDescriptor(e,i):s;if("object"==typeof Reflect&&"function"==typeof Reflect.decorate)n=Reflect.decorate(t,e,i,s);else for(var l=t.length-1;l>=0;l--)(o=t[l])&&(n=(r<3?o(n):r>3?o(e,i,n):o(e,i))||n);return r>3&&n&&Object.defineProperty(e,i,n),n
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */}const e=t=>e=>"function"==typeof e?((t,e)=>(customElements.define(t,e),e))(t,e):((t,e)=>{const{kind:i,elements:s}=e;return{kind:i,elements:s,finisher(e){customElements.define(t,e)}}})(t,e)
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */,i=(t,e)=>"method"===e.kind&&e.descriptor&&!("value"in e.descriptor)?{...e,finisher(i){i.createProperty(e.key,t)}}:{kind:"field",key:Symbol(),placement:"own",descriptor:{},originalKey:e.key,initializer(){"function"==typeof e.initializer&&(this[e.key]=e.initializer.call(this))},finisher(i){i.createProperty(e.key,t)}};function s(t){return(e,s)=>void 0!==s?((t,e,i)=>{e.constructor.createProperty(i,t)})(t,e,s):i(t,e)
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */}const o=({finisher:t,descriptor:e})=>(i,s)=>{var o;if(void 0===s){const s=null!==(o=i.originalKey)&&void 0!==o?o:i.key,r=null!=e?{kind:"method",placement:"prototype",key:s,descriptor:e(i.key)}:{...i,key:s};return null!=t&&(r.finisher=function(e){t(e,s)}),r}{const o=i.constructor;void 0!==e&&Object.defineProperty(i,s,e(s)),null==t||t(o,s)}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;function r(t,e){return o({descriptor:i=>{const s={get(){var e,i;return null!==(i=null===(e=this.renderRoot)||void 0===e?void 0:e.querySelector(t))&&void 0!==i?i:null},enumerable:!0,configurable:!0};if(e){const e="symbol"==typeof i?Symbol():"__"+i;s.get=function(){var i,s;return void 0===this[e]&&(this[e]=null!==(s=null===(i=this.renderRoot)||void 0===i?void 0:i.querySelector(t))&&void 0!==s?s:null),this[e]}}return s}})}
/**
 * @license
 * Copyright 2021 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var n;const l=null!=(null===(n=window.HTMLSlotElement)||void 0===n?void 0:n.prototype.assignedElements)?(t,e)=>t.assignedElements(e):(t,e)=>t.assignedNodes(e).filter((t=>t.nodeType===Node.ELEMENT_NODE));function a(t){const{slot:e,selector:i}=null!=t?t:{};return o({descriptor:s=>({get(){var s;const o="slot"+(e?`[name=${e}]`:":not([name])"),r=null===(s=this.renderRoot)||void 0===s?void 0:s.querySelector(o),n=null!=r?l(r,t):[];return i?n.filter((t=>t.matches(i))):n},enumerable:!0,configurable:!0})})}
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const h=window.ShadowRoot&&(void 0===window.ShadyCSS||window.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,d=Symbol(),c=new Map;class u{constructor(t,e){if(this._$cssResult$=!0,e!==d)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t}get styleSheet(){let t=c.get(this.cssText);return h&&void 0===t&&(c.set(this.cssText,t=new CSSStyleSheet),t.replaceSync(this.cssText)),t}toString(){return this.cssText}}const p=(t,...e)=>{const i=1===t.length?t[0]:e.reduce(((e,i,s)=>e+(t=>{if(!0===t._$cssResult$)return t.cssText;if("number"==typeof t)return t;throw Error("Value passed to 'css' function must be a 'css' function result: "+t+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(i)+t[s+1]),t[0]);return new u(i,d)},$=h?t=>t:t=>t instanceof CSSStyleSheet?(t=>{let e="";for(const i of t.cssRules)e+=i.cssText;return(t=>new u("string"==typeof t?t:t+"",d))(e)})(t):t
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;var v;const f=window.trustedTypes,_=f?f.emptyScript:"",g=window.reactiveElementPolyfillSupport,m={toAttribute(t,e){switch(e){case Boolean:t=t?_:null;break;case Object:case Array:t=null==t?t:JSON.stringify(t)}return t},fromAttribute(t,e){let i=t;switch(e){case Boolean:i=null!==t;break;case Number:i=null===t?null:Number(t);break;case Object:case Array:try{i=JSON.parse(t)}catch(t){i=null}}return i}},y=(t,e)=>e!==t&&(e==e||t==t),b={attribute:!0,type:String,converter:m,reflect:!1,hasChanged:y};class A extends HTMLElement{constructor(){super(),this._$Et=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Ei=null,this.o()}static addInitializer(t){var e;null!==(e=this.l)&&void 0!==e||(this.l=[]),this.l.push(t)}static get observedAttributes(){this.finalize();const t=[];return this.elementProperties.forEach(((e,i)=>{const s=this._$Eh(i,e);void 0!==s&&(this._$Eu.set(s,i),t.push(s))})),t}static createProperty(t,e=b){if(e.state&&(e.attribute=!1),this.finalize(),this.elementProperties.set(t,e),!e.noAccessor&&!this.prototype.hasOwnProperty(t)){const i="symbol"==typeof t?Symbol():"__"+t,s=this.getPropertyDescriptor(t,i,e);void 0!==s&&Object.defineProperty(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){return{get(){return this[e]},set(s){const o=this[t];this[e]=s,this.requestUpdate(t,o,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)||b}static finalize(){if(this.hasOwnProperty("finalized"))return!1;this.finalized=!0;const t=Object.getPrototypeOf(this);if(t.finalize(),this.elementProperties=new Map(t.elementProperties),this._$Eu=new Map,this.hasOwnProperty("properties")){const t=this.properties,e=[...Object.getOwnPropertyNames(t),...Object.getOwnPropertySymbols(t)];for(const i of e)this.createProperty(i,t[i])}return this.elementStyles=this.finalizeStyles(this.styles),!0}static finalizeStyles(t){const e=[];if(Array.isArray(t)){const i=new Set(t.flat(1/0).reverse());for(const t of i)e.unshift($(t))}else void 0!==t&&e.push($(t));return e}static _$Eh(t,e){const i=e.attribute;return!1===i?void 0:"string"==typeof i?i:"string"==typeof t?t.toLowerCase():void 0}o(){var t;this._$Ep=new Promise((t=>this.enableUpdating=t)),this._$AL=new Map,this._$Em(),this.requestUpdate(),null===(t=this.constructor.l)||void 0===t||t.forEach((t=>t(this)))}addController(t){var e,i;(null!==(e=this._$Eg)&&void 0!==e?e:this._$Eg=[]).push(t),void 0!==this.renderRoot&&this.isConnected&&(null===(i=t.hostConnected)||void 0===i||i.call(t))}removeController(t){var e;null===(e=this._$Eg)||void 0===e||e.splice(this._$Eg.indexOf(t)>>>0,1)}_$Em(){this.constructor.elementProperties.forEach(((t,e)=>{this.hasOwnProperty(e)&&(this._$Et.set(e,this[e]),delete this[e])}))}createRenderRoot(){var t;const e=null!==(t=this.shadowRoot)&&void 0!==t?t:this.attachShadow(this.constructor.shadowRootOptions);return((t,e)=>{h?t.adoptedStyleSheets=e.map((t=>t instanceof CSSStyleSheet?t:t.styleSheet)):e.forEach((e=>{const i=document.createElement("style"),s=window.litNonce;void 0!==s&&i.setAttribute("nonce",s),i.textContent=e.cssText,t.appendChild(i)}))})(e,this.constructor.elementStyles),e}connectedCallback(){var t;void 0===this.renderRoot&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),null===(t=this._$Eg)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostConnected)||void 0===e?void 0:e.call(t)}))}enableUpdating(t){}disconnectedCallback(){var t;null===(t=this._$Eg)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostDisconnected)||void 0===e?void 0:e.call(t)}))}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$ES(t,e,i=b){var s,o;const r=this.constructor._$Eh(t,i);if(void 0!==r&&!0===i.reflect){const n=(null!==(o=null===(s=i.converter)||void 0===s?void 0:s.toAttribute)&&void 0!==o?o:m.toAttribute)(e,i.type);this._$Ei=t,null==n?this.removeAttribute(r):this.setAttribute(r,n),this._$Ei=null}}_$AK(t,e){var i,s,o;const r=this.constructor,n=r._$Eu.get(t);if(void 0!==n&&this._$Ei!==n){const t=r.getPropertyOptions(n),l=t.converter,a=null!==(o=null!==(s=null===(i=l)||void 0===i?void 0:i.fromAttribute)&&void 0!==s?s:"function"==typeof l?l:null)&&void 0!==o?o:m.fromAttribute;this._$Ei=n,this[n]=a(e,t.type),this._$Ei=null}}requestUpdate(t,e,i){let s=!0;void 0!==t&&(((i=i||this.constructor.getPropertyOptions(t)).hasChanged||y)(this[t],e)?(this._$AL.has(t)||this._$AL.set(t,e),!0===i.reflect&&this._$Ei!==t&&(void 0===this._$EC&&(this._$EC=new Map),this._$EC.set(t,i))):s=!1),!this.isUpdatePending&&s&&(this._$Ep=this._$E_())}async _$E_(){this.isUpdatePending=!0;try{await this._$Ep}catch(t){Promise.reject(t)}const t=this.scheduleUpdate();return null!=t&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var t;if(!this.isUpdatePending)return;this.hasUpdated,this._$Et&&(this._$Et.forEach(((t,e)=>this[e]=t)),this._$Et=void 0);let e=!1;const i=this._$AL;try{e=this.shouldUpdate(i),e?(this.willUpdate(i),null===(t=this._$Eg)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostUpdate)||void 0===e?void 0:e.call(t)})),this.update(i)):this._$EU()}catch(t){throw e=!1,this._$EU(),t}e&&this._$AE(i)}willUpdate(t){}_$AE(t){var e;null===(e=this._$Eg)||void 0===e||e.forEach((t=>{var e;return null===(e=t.hostUpdated)||void 0===e?void 0:e.call(t)})),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EU(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$Ep}shouldUpdate(t){return!0}update(t){void 0!==this._$EC&&(this._$EC.forEach(((t,e)=>this._$ES(e,this[e],t))),this._$EC=void 0),this._$EU()}updated(t){}firstUpdated(t){}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var E;A.finalized=!0,A.elementProperties=new Map,A.elementStyles=[],A.shadowRootOptions={mode:"open"},null==g||g({ReactiveElement:A}),(null!==(v=globalThis.reactiveElementVersions)&&void 0!==v?v:globalThis.reactiveElementVersions=[]).push("1.3.1");const S=globalThis.trustedTypes,w=S?S.createPolicy("lit-html",{createHTML:t=>t}):void 0,C=`lit$${(Math.random()+"").slice(9)}$`,x="?"+C,U=`<${x}>`,k=document,P=(t="")=>k.createComment(t),H=t=>null===t||"object"!=typeof t&&"function"!=typeof t,T=Array.isArray,O=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,N=/-->/g,R=/>/g,M=/>|[ 	\n\r](?:([^\s"'>=/]+)([ 	\n\r]*=[ 	\n\r]*(?:[^ 	\n\r"'`<>=]|("|')|))|$)/g,z=/'/g,L=/"/g,B=/^(?:script|style|textarea|title)$/i,D=Symbol.for("lit-noChange"),j=Symbol.for("lit-nothing"),I=new WeakMap,V=k.createTreeWalker(k,129,null,!1);class W{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let o=0,r=0;const n=t.length-1,l=this.parts,[a,h]=((t,e)=>{const i=t.length-1,s=[];let o,r=2===e?"<svg>":"",n=O;for(let e=0;e<i;e++){const i=t[e];let l,a,h=-1,d=0;for(;d<i.length&&(n.lastIndex=d,a=n.exec(i),null!==a);)d=n.lastIndex,n===O?"!--"===a[1]?n=N:void 0!==a[1]?n=R:void 0!==a[2]?(B.test(a[2])&&(o=RegExp("</"+a[2],"g")),n=M):void 0!==a[3]&&(n=M):n===M?">"===a[0]?(n=null!=o?o:O,h=-1):void 0===a[1]?h=-2:(h=n.lastIndex-a[2].length,l=a[1],n=void 0===a[3]?M:'"'===a[3]?L:z):n===L||n===z?n=M:n===N||n===R?n=O:(n=M,o=void 0);const c=n===M&&t[e+1].startsWith("/>")?" ":"";r+=n===O?i+U:h>=0?(s.push(l),i.slice(0,h)+"$lit$"+i.slice(h)+C+c):i+C+(-2===h?(s.push(void 0),e):c)}const l=r+(t[i]||"<?>")+(2===e?"</svg>":"");if(!Array.isArray(t)||!t.hasOwnProperty("raw"))throw Error("invalid template strings array");return[void 0!==w?w.createHTML(l):l,s]})(t,e);if(this.el=W.createElement(a,i),V.currentNode=this.el.content,2===e){const t=this.el.content,e=t.firstChild;e.remove(),t.append(...e.childNodes)}for(;null!==(s=V.nextNode())&&l.length<n;){if(1===s.nodeType){if(s.hasAttributes()){const t=[];for(const e of s.getAttributeNames())if(e.endsWith("$lit$")||e.startsWith(C)){const i=h[r++];if(t.push(e),void 0!==i){const t=s.getAttribute(i.toLowerCase()+"$lit$").split(C),e=/([.?@])?(.*)/.exec(i);l.push({type:1,index:o,name:e[2],strings:t,ctor:"."===e[1]?G:"?"===e[1]?Q:"@"===e[1]?X:Z})}else l.push({type:6,index:o})}for(const e of t)s.removeAttribute(e)}if(B.test(s.tagName)){const t=s.textContent.split(C),e=t.length-1;if(e>0){s.textContent=S?S.emptyScript:"";for(let i=0;i<e;i++)s.append(t[i],P()),V.nextNode(),l.push({type:2,index:++o});s.append(t[e],P())}}}else if(8===s.nodeType)if(s.data===x)l.push({type:2,index:o});else{let t=-1;for(;-1!==(t=s.data.indexOf(C,t+1));)l.push({type:7,index:o}),t+=C.length-1}o++}}static createElement(t,e){const i=k.createElement("template");return i.innerHTML=t,i}}function q(t,e,i=t,s){var o,r,n,l;if(e===D)return e;let a=void 0!==s?null===(o=i._$Cl)||void 0===o?void 0:o[s]:i._$Cu;const h=H(e)?void 0:e._$litDirective$;return(null==a?void 0:a.constructor)!==h&&(null===(r=null==a?void 0:a._$AO)||void 0===r||r.call(a,!1),void 0===h?a=void 0:(a=new h(t),a._$AT(t,i,s)),void 0!==s?(null!==(n=(l=i)._$Cl)&&void 0!==n?n:l._$Cl=[])[s]=a:i._$Cu=a),void 0!==a&&(e=q(t,a._$AS(t,e.values),a,s)),e}class K{constructor(t,e){this.v=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}p(t){var e;const{el:{content:i},parts:s}=this._$AD,o=(null!==(e=null==t?void 0:t.creationScope)&&void 0!==e?e:k).importNode(i,!0);V.currentNode=o;let r=V.nextNode(),n=0,l=0,a=s[0];for(;void 0!==a;){if(n===a.index){let e;2===a.type?e=new J(r,r.nextSibling,this,t):1===a.type?e=new a.ctor(r,a.name,a.strings,this,t):6===a.type&&(e=new Y(r,this,t)),this.v.push(e),a=s[++l]}n!==(null==a?void 0:a.index)&&(r=V.nextNode(),n++)}return o}m(t){let e=0;for(const i of this.v)void 0!==i&&(void 0!==i.strings?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}}class J{constructor(t,e,i,s){var o;this.type=2,this._$AH=j,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cg=null===(o=null==s?void 0:s.isConnected)||void 0===o||o}get _$AU(){var t,e;return null!==(e=null===(t=this._$AM)||void 0===t?void 0:t._$AU)&&void 0!==e?e:this._$Cg}get parentNode(){let t=this._$AA.parentNode;const e=this._$AM;return void 0!==e&&11===t.nodeType&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=q(this,t,e),H(t)?t===j||null==t||""===t?(this._$AH!==j&&this._$AR(),this._$AH=j):t!==this._$AH&&t!==D&&this.$(t):void 0!==t._$litType$?this.T(t):void 0!==t.nodeType?this.k(t):(t=>{var e;return T(t)||"function"==typeof(null===(e=t)||void 0===e?void 0:e[Symbol.iterator])})(t)?this.S(t):this.$(t)}M(t,e=this._$AB){return this._$AA.parentNode.insertBefore(t,e)}k(t){this._$AH!==t&&(this._$AR(),this._$AH=this.M(t))}$(t){this._$AH!==j&&H(this._$AH)?this._$AA.nextSibling.data=t:this.k(k.createTextNode(t)),this._$AH=t}T(t){var e;const{values:i,_$litType$:s}=t,o="number"==typeof s?this._$AC(t):(void 0===s.el&&(s.el=W.createElement(s.h,this.options)),s);if((null===(e=this._$AH)||void 0===e?void 0:e._$AD)===o)this._$AH.m(i);else{const t=new K(o,this),e=t.p(this.options);t.m(i),this.k(e),this._$AH=t}}_$AC(t){let e=I.get(t.strings);return void 0===e&&I.set(t.strings,e=new W(t)),e}S(t){T(this._$AH)||(this._$AH=[],this._$AR());const e=this._$AH;let i,s=0;for(const o of t)s===e.length?e.push(i=new J(this.M(P()),this.M(P()),this,this.options)):i=e[s],i._$AI(o),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){var i;for(null===(i=this._$AP)||void 0===i||i.call(this,!1,!0,e);t&&t!==this._$AB;){const e=t.nextSibling;t.remove(),t=e}}setConnected(t){var e;void 0===this._$AM&&(this._$Cg=t,null===(e=this._$AP)||void 0===e||e.call(this,t))}}class Z{constructor(t,e,i,s,o){this.type=1,this._$AH=j,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=o,i.length>2||""!==i[0]||""!==i[1]?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=j}get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}_$AI(t,e=this,i,s){const o=this.strings;let r=!1;if(void 0===o)t=q(this,t,e,0),r=!H(t)||t!==this._$AH&&t!==D,r&&(this._$AH=t);else{const s=t;let n,l;for(t=o[0],n=0;n<o.length-1;n++)l=q(this,s[i+n],e,n),l===D&&(l=this._$AH[n]),r||(r=!H(l)||l!==this._$AH[n]),l===j?t=j:t!==j&&(t+=(null!=l?l:"")+o[n+1]),this._$AH[n]=l}r&&!s&&this.C(t)}C(t){t===j?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,null!=t?t:"")}}class G extends Z{constructor(){super(...arguments),this.type=3}C(t){this.element[this.name]=t===j?void 0:t}}const F=S?S.emptyScript:"";class Q extends Z{constructor(){super(...arguments),this.type=4}C(t){t&&t!==j?this.element.setAttribute(this.name,F):this.element.removeAttribute(this.name)}}class X extends Z{constructor(t,e,i,s,o){super(t,e,i,s,o),this.type=5}_$AI(t,e=this){var i;if((t=null!==(i=q(this,t,e,0))&&void 0!==i?i:j)===D)return;const s=this._$AH,o=t===j&&s!==j||t.capture!==s.capture||t.once!==s.once||t.passive!==s.passive,r=t!==j&&(s===j||o);o&&this.element.removeEventListener(this.name,this,s),r&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){var e,i;"function"==typeof this._$AH?this._$AH.call(null!==(i=null===(e=this.options)||void 0===e?void 0:e.host)&&void 0!==i?i:this.element,t):this._$AH.handleEvent(t)}}class Y{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){q(this,t)}}const tt=window.litHtmlPolyfillSupport;
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var et,it;null==tt||tt(W,J),(null!==(E=globalThis.litHtmlVersions)&&void 0!==E?E:globalThis.litHtmlVersions=[]).push("2.2.2");class st extends A{constructor(){super(...arguments),this.renderOptions={host:this},this._$Dt=void 0}createRenderRoot(){var t,e;const i=super.createRenderRoot();return null!==(t=(e=this.renderOptions).renderBefore)&&void 0!==t||(e.renderBefore=i.firstChild),i}update(t){const e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Dt=((t,e,i)=>{var s,o;const r=null!==(s=null==i?void 0:i.renderBefore)&&void 0!==s?s:e;let n=r._$litPart$;if(void 0===n){const t=null!==(o=null==i?void 0:i.renderBefore)&&void 0!==o?o:null;r._$litPart$=n=new J(e.insertBefore(P(),t),t,void 0,null!=i?i:{})}return n._$AI(t),n})(e,this.renderRoot,this.renderOptions)}connectedCallback(){var t;super.connectedCallback(),null===(t=this._$Dt)||void 0===t||t.setConnected(!0)}disconnectedCallback(){var t;super.disconnectedCallback(),null===(t=this._$Dt)||void 0===t||t.setConnected(!1)}render(){return D}}st.finalized=!0,st._$litElement$=!0,null===(et=globalThis.litElementHydrateSupport)||void 0===et||et.call(globalThis,{LitElement:st});const ot=globalThis.litElementPolyfillSupport;null==ot||ot({LitElement:st}),(null!==(it=globalThis.litElementVersions)&&void 0!==it?it:globalThis.litElementVersions=[]).push("3.2.0");const rt=p`#2b74a1`,nt=p`#1b4865`,lt=p`#f0f0f0`,at=p`#222`,ht=rt,dt=p`#fff`,ct=p`#1e7b34`,ut=p`#fff`;p`#e3e7e8`;const pt={backgroundColor:ht,border:p`none`,color:dt,cursor:p`pointer`,hoverBackgroundColor:nt,hoverColor:dt,transition:p`background-color 300ms ease-out`};var $t=p`
  :host {
    /* DataTable action buttons */
    --data-table-action-button-background-color: ${pt.backgroundColor};
    --data-table-action-button-border: ${pt.border};
    --data-table-action-button-color: ${pt.color};
    --data-table-action-button-cursor: ${pt.cursor};
    --data-table-action-button-hover-background-color: ${pt.hoverBackgroundColor};
    --data-table-action-button-hover-color: ${pt.hoverColor};
    --data-table-action-button-transition: ${pt.transition};

    /* DataTable paginator */
    --data-table-paginator-wrapper-font-size: 1rem;
    --data-table-paginator-control-button-background-color: transparent;
    --data-table-paginator-control-button-border: none;
    --data-table-paginator-control-button-color: #348fc6;
    --data-table-paginator-control-button-padding: 0.25rem;
  }

  a:any-link {
    color: ${rt};
  }

  a:hover {
    color: ${nt};
  }
`
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;const vt=window,ft=vt.ShadowRoot&&(void 0===vt.ShadyCSS||vt.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,_t=Symbol(),gt=new WeakMap;class mt{constructor(t,e,i){if(this._$cssResult$=!0,i!==_t)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o;const e=this.t;if(ft&&void 0===t){const i=void 0!==e&&1===e.length;i&&(t=gt.get(e)),void 0===t&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),i&&gt.set(e,t))}return t}toString(){return this.cssText}}const yt=(t,...e)=>{const i=1===t.length?t[0]:e.reduce(((e,i,s)=>e+(t=>{if(!0===t._$cssResult$)return t.cssText;if("number"==typeof t)return t;throw Error("Value passed to 'css' function must be a 'css' function result: "+t+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(i)+t[s+1]),t[0]);return new mt(i,t,_t)},bt=ft?t=>t:t=>t instanceof CSSStyleSheet?(t=>{let e="";for(const i of t.cssRules)e+=i.cssText;return(t=>new mt("string"==typeof t?t:t+"",void 0,_t))(e)})(t):t
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;var At;const Et=window,St=Et.trustedTypes,wt=St?St.emptyScript:"",Ct=Et.reactiveElementPolyfillSupport,xt={toAttribute(t,e){switch(e){case Boolean:t=t?wt:null;break;case Object:case Array:t=null==t?t:JSON.stringify(t)}return t},fromAttribute(t,e){let i=t;switch(e){case Boolean:i=null!==t;break;case Number:i=null===t?null:Number(t);break;case Object:case Array:try{i=JSON.parse(t)}catch(t){i=null}}return i}},Ut=(t,e)=>e!==t&&(e==e||t==t),kt={attribute:!0,type:String,converter:xt,reflect:!1,hasChanged:Ut};class Pt extends HTMLElement{constructor(){super(),this._$Ei=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this._$El=null,this.u()}static addInitializer(t){var e;this.finalize(),(null!==(e=this.h)&&void 0!==e?e:this.h=[]).push(t)}static get observedAttributes(){this.finalize();const t=[];return this.elementProperties.forEach(((e,i)=>{const s=this._$Ep(i,e);void 0!==s&&(this._$Ev.set(s,i),t.push(s))})),t}static createProperty(t,e=kt){if(e.state&&(e.attribute=!1),this.finalize(),this.elementProperties.set(t,e),!e.noAccessor&&!this.prototype.hasOwnProperty(t)){const i="symbol"==typeof t?Symbol():"__"+t,s=this.getPropertyDescriptor(t,i,e);void 0!==s&&Object.defineProperty(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){return{get(){return this[e]},set(s){const o=this[t];this[e]=s,this.requestUpdate(t,o,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)||kt}static finalize(){if(this.hasOwnProperty("finalized"))return!1;this.finalized=!0;const t=Object.getPrototypeOf(this);if(t.finalize(),void 0!==t.h&&(this.h=[...t.h]),this.elementProperties=new Map(t.elementProperties),this._$Ev=new Map,this.hasOwnProperty("properties")){const t=this.properties,e=[...Object.getOwnPropertyNames(t),...Object.getOwnPropertySymbols(t)];for(const i of e)this.createProperty(i,t[i])}return this.elementStyles=this.finalizeStyles(this.styles),!0}static finalizeStyles(t){const e=[];if(Array.isArray(t)){const i=new Set(t.flat(1/0).reverse());for(const t of i)e.unshift(bt(t))}else void 0!==t&&e.push(bt(t));return e}static _$Ep(t,e){const i=e.attribute;return!1===i?void 0:"string"==typeof i?i:"string"==typeof t?t.toLowerCase():void 0}u(){var t;this._$E_=new Promise((t=>this.enableUpdating=t)),this._$AL=new Map,this._$Eg(),this.requestUpdate(),null===(t=this.constructor.h)||void 0===t||t.forEach((t=>t(this)))}addController(t){var e,i;(null!==(e=this._$ES)&&void 0!==e?e:this._$ES=[]).push(t),void 0!==this.renderRoot&&this.isConnected&&(null===(i=t.hostConnected)||void 0===i||i.call(t))}removeController(t){var e;null===(e=this._$ES)||void 0===e||e.splice(this._$ES.indexOf(t)>>>0,1)}_$Eg(){this.constructor.elementProperties.forEach(((t,e)=>{this.hasOwnProperty(e)&&(this._$Ei.set(e,this[e]),delete this[e])}))}createRenderRoot(){var t;const e=null!==(t=this.shadowRoot)&&void 0!==t?t:this.attachShadow(this.constructor.shadowRootOptions);return((t,e)=>{ft?t.adoptedStyleSheets=e.map((t=>t instanceof CSSStyleSheet?t:t.styleSheet)):e.forEach((e=>{const i=document.createElement("style"),s=vt.litNonce;void 0!==s&&i.setAttribute("nonce",s),i.textContent=e.cssText,t.appendChild(i)}))})(e,this.constructor.elementStyles),e}connectedCallback(){var t;void 0===this.renderRoot&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),null===(t=this._$ES)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostConnected)||void 0===e?void 0:e.call(t)}))}enableUpdating(t){}disconnectedCallback(){var t;null===(t=this._$ES)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostDisconnected)||void 0===e?void 0:e.call(t)}))}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$EO(t,e,i=kt){var s;const o=this.constructor._$Ep(t,i);if(void 0!==o&&!0===i.reflect){const r=(void 0!==(null===(s=i.converter)||void 0===s?void 0:s.toAttribute)?i.converter:xt).toAttribute(e,i.type);this._$El=t,null==r?this.removeAttribute(o):this.setAttribute(o,r),this._$El=null}}_$AK(t,e){var i;const s=this.constructor,o=s._$Ev.get(t);if(void 0!==o&&this._$El!==o){const t=s.getPropertyOptions(o),r="function"==typeof t.converter?{fromAttribute:t.converter}:void 0!==(null===(i=t.converter)||void 0===i?void 0:i.fromAttribute)?t.converter:xt;this._$El=o,this[o]=r.fromAttribute(e,t.type),this._$El=null}}requestUpdate(t,e,i){let s=!0;void 0!==t&&(((i=i||this.constructor.getPropertyOptions(t)).hasChanged||Ut)(this[t],e)?(this._$AL.has(t)||this._$AL.set(t,e),!0===i.reflect&&this._$El!==t&&(void 0===this._$EC&&(this._$EC=new Map),this._$EC.set(t,i))):s=!1),!this.isUpdatePending&&s&&(this._$E_=this._$Ej())}async _$Ej(){this.isUpdatePending=!0;try{await this._$E_}catch(t){Promise.reject(t)}const t=this.scheduleUpdate();return null!=t&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var t;if(!this.isUpdatePending)return;this.hasUpdated,this._$Ei&&(this._$Ei.forEach(((t,e)=>this[e]=t)),this._$Ei=void 0);let e=!1;const i=this._$AL;try{e=this.shouldUpdate(i),e?(this.willUpdate(i),null===(t=this._$ES)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostUpdate)||void 0===e?void 0:e.call(t)})),this.update(i)):this._$Ek()}catch(t){throw e=!1,this._$Ek(),t}e&&this._$AE(i)}willUpdate(t){}_$AE(t){var e;null===(e=this._$ES)||void 0===e||e.forEach((t=>{var e;return null===(e=t.hostUpdated)||void 0===e?void 0:e.call(t)})),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$Ek(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$E_}shouldUpdate(t){return!0}update(t){void 0!==this._$EC&&(this._$EC.forEach(((t,e)=>this._$EO(e,this[e],t))),this._$EC=void 0),this._$Ek()}updated(t){}firstUpdated(t){}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var Ht;Pt.finalized=!0,Pt.elementProperties=new Map,Pt.elementStyles=[],Pt.shadowRootOptions={mode:"open"},null==Ct||Ct({ReactiveElement:Pt}),(null!==(At=Et.reactiveElementVersions)&&void 0!==At?At:Et.reactiveElementVersions=[]).push("1.6.1");const Tt=window,Ot=Tt.trustedTypes,Nt=Ot?Ot.createPolicy("lit-html",{createHTML:t=>t}):void 0,Rt=`lit$${(Math.random()+"").slice(9)}$`,Mt="?"+Rt,zt=`<${Mt}>`,Lt=document,Bt=(t="")=>Lt.createComment(t),Dt=t=>null===t||"object"!=typeof t&&"function"!=typeof t,jt=Array.isArray,It=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,Vt=/-->/g,Wt=/>/g,qt=RegExp(">|[ \t\n\f\r](?:([^\\s\"'>=/]+)([ \t\n\f\r]*=[ \t\n\f\r]*(?:[^ \t\n\f\r\"'`<>=]|(\"|')|))|$)","g"),Kt=/'/g,Jt=/"/g,Zt=/^(?:script|style|textarea|title)$/i,Gt=(t=>(e,...i)=>({_$litType$:t,strings:e,values:i}))(1),Ft=Symbol.for("lit-noChange"),Qt=Symbol.for("lit-nothing"),Xt=new WeakMap,Yt=Lt.createTreeWalker(Lt,129,null,!1),te=(t,e)=>{const i=t.length-1,s=[];let o,r=2===e?"<svg>":"",n=It;for(let e=0;e<i;e++){const i=t[e];let l,a,h=-1,d=0;for(;d<i.length&&(n.lastIndex=d,a=n.exec(i),null!==a);)d=n.lastIndex,n===It?"!--"===a[1]?n=Vt:void 0!==a[1]?n=Wt:void 0!==a[2]?(Zt.test(a[2])&&(o=RegExp("</"+a[2],"g")),n=qt):void 0!==a[3]&&(n=qt):n===qt?">"===a[0]?(n=null!=o?o:It,h=-1):void 0===a[1]?h=-2:(h=n.lastIndex-a[2].length,l=a[1],n=void 0===a[3]?qt:'"'===a[3]?Jt:Kt):n===Jt||n===Kt?n=qt:n===Vt||n===Wt?n=It:(n=qt,o=void 0);const c=n===qt&&t[e+1].startsWith("/>")?" ":"";r+=n===It?i+zt:h>=0?(s.push(l),i.slice(0,h)+"$lit$"+i.slice(h)+Rt+c):i+Rt+(-2===h?(s.push(void 0),e):c)}const l=r+(t[i]||"<?>")+(2===e?"</svg>":"");if(!Array.isArray(t)||!t.hasOwnProperty("raw"))throw Error("invalid template strings array");return[void 0!==Nt?Nt.createHTML(l):l,s]};class ee{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let o=0,r=0;const n=t.length-1,l=this.parts,[a,h]=te(t,e);if(this.el=ee.createElement(a,i),Yt.currentNode=this.el.content,2===e){const t=this.el.content,e=t.firstChild;e.remove(),t.append(...e.childNodes)}for(;null!==(s=Yt.nextNode())&&l.length<n;){if(1===s.nodeType){if(s.hasAttributes()){const t=[];for(const e of s.getAttributeNames())if(e.endsWith("$lit$")||e.startsWith(Rt)){const i=h[r++];if(t.push(e),void 0!==i){const t=s.getAttribute(i.toLowerCase()+"$lit$").split(Rt),e=/([.?@])?(.*)/.exec(i);l.push({type:1,index:o,name:e[2],strings:t,ctor:"."===e[1]?ne:"?"===e[1]?ae:"@"===e[1]?he:re})}else l.push({type:6,index:o})}for(const e of t)s.removeAttribute(e)}if(Zt.test(s.tagName)){const t=s.textContent.split(Rt),e=t.length-1;if(e>0){s.textContent=Ot?Ot.emptyScript:"";for(let i=0;i<e;i++)s.append(t[i],Bt()),Yt.nextNode(),l.push({type:2,index:++o});s.append(t[e],Bt())}}}else if(8===s.nodeType)if(s.data===Mt)l.push({type:2,index:o});else{let t=-1;for(;-1!==(t=s.data.indexOf(Rt,t+1));)l.push({type:7,index:o}),t+=Rt.length-1}o++}}static createElement(t,e){const i=Lt.createElement("template");return i.innerHTML=t,i}}function ie(t,e,i=t,s){var o,r,n,l;if(e===Ft)return e;let a=void 0!==s?null===(o=i._$Co)||void 0===o?void 0:o[s]:i._$Cl;const h=Dt(e)?void 0:e._$litDirective$;return(null==a?void 0:a.constructor)!==h&&(null===(r=null==a?void 0:a._$AO)||void 0===r||r.call(a,!1),void 0===h?a=void 0:(a=new h(t),a._$AT(t,i,s)),void 0!==s?(null!==(n=(l=i)._$Co)&&void 0!==n?n:l._$Co=[])[s]=a:i._$Cl=a),void 0!==a&&(e=ie(t,a._$AS(t,e.values),a,s)),e}class se{constructor(t,e){this.u=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}v(t){var e;const{el:{content:i},parts:s}=this._$AD,o=(null!==(e=null==t?void 0:t.creationScope)&&void 0!==e?e:Lt).importNode(i,!0);Yt.currentNode=o;let r=Yt.nextNode(),n=0,l=0,a=s[0];for(;void 0!==a;){if(n===a.index){let e;2===a.type?e=new oe(r,r.nextSibling,this,t):1===a.type?e=new a.ctor(r,a.name,a.strings,this,t):6===a.type&&(e=new de(r,this,t)),this.u.push(e),a=s[++l]}n!==(null==a?void 0:a.index)&&(r=Yt.nextNode(),n++)}return o}p(t){let e=0;for(const i of this.u)void 0!==i&&(void 0!==i.strings?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}}class oe{constructor(t,e,i,s){var o;this.type=2,this._$AH=Qt,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cm=null===(o=null==s?void 0:s.isConnected)||void 0===o||o}get _$AU(){var t,e;return null!==(e=null===(t=this._$AM)||void 0===t?void 0:t._$AU)&&void 0!==e?e:this._$Cm}get parentNode(){let t=this._$AA.parentNode;const e=this._$AM;return void 0!==e&&11===t.nodeType&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=ie(this,t,e),Dt(t)?t===Qt||null==t||""===t?(this._$AH!==Qt&&this._$AR(),this._$AH=Qt):t!==this._$AH&&t!==Ft&&this.g(t):void 0!==t._$litType$?this.$(t):void 0!==t.nodeType?this.T(t):(t=>jt(t)||"function"==typeof(null==t?void 0:t[Symbol.iterator]))(t)?this.k(t):this.g(t)}O(t,e=this._$AB){return this._$AA.parentNode.insertBefore(t,e)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}g(t){this._$AH!==Qt&&Dt(this._$AH)?this._$AA.nextSibling.data=t:this.T(Lt.createTextNode(t)),this._$AH=t}$(t){var e;const{values:i,_$litType$:s}=t,o="number"==typeof s?this._$AC(t):(void 0===s.el&&(s.el=ee.createElement(s.h,this.options)),s);if((null===(e=this._$AH)||void 0===e?void 0:e._$AD)===o)this._$AH.p(i);else{const t=new se(o,this),e=t.v(this.options);t.p(i),this.T(e),this._$AH=t}}_$AC(t){let e=Xt.get(t.strings);return void 0===e&&Xt.set(t.strings,e=new ee(t)),e}k(t){jt(this._$AH)||(this._$AH=[],this._$AR());const e=this._$AH;let i,s=0;for(const o of t)s===e.length?e.push(i=new oe(this.O(Bt()),this.O(Bt()),this,this.options)):i=e[s],i._$AI(o),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){var i;for(null===(i=this._$AP)||void 0===i||i.call(this,!1,!0,e);t&&t!==this._$AB;){const e=t.nextSibling;t.remove(),t=e}}setConnected(t){var e;void 0===this._$AM&&(this._$Cm=t,null===(e=this._$AP)||void 0===e||e.call(this,t))}}class re{constructor(t,e,i,s,o){this.type=1,this._$AH=Qt,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=o,i.length>2||""!==i[0]||""!==i[1]?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=Qt}get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}_$AI(t,e=this,i,s){const o=this.strings;let r=!1;if(void 0===o)t=ie(this,t,e,0),r=!Dt(t)||t!==this._$AH&&t!==Ft,r&&(this._$AH=t);else{const s=t;let n,l;for(t=o[0],n=0;n<o.length-1;n++)l=ie(this,s[i+n],e,n),l===Ft&&(l=this._$AH[n]),r||(r=!Dt(l)||l!==this._$AH[n]),l===Qt?t=Qt:t!==Qt&&(t+=(null!=l?l:"")+o[n+1]),this._$AH[n]=l}r&&!s&&this.j(t)}j(t){t===Qt?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,null!=t?t:"")}}class ne extends re{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===Qt?void 0:t}}const le=Ot?Ot.emptyScript:"";class ae extends re{constructor(){super(...arguments),this.type=4}j(t){t&&t!==Qt?this.element.setAttribute(this.name,le):this.element.removeAttribute(this.name)}}class he extends re{constructor(t,e,i,s,o){super(t,e,i,s,o),this.type=5}_$AI(t,e=this){var i;if((t=null!==(i=ie(this,t,e,0))&&void 0!==i?i:Qt)===Ft)return;const s=this._$AH,o=t===Qt&&s!==Qt||t.capture!==s.capture||t.once!==s.once||t.passive!==s.passive,r=t!==Qt&&(s===Qt||o);o&&this.element.removeEventListener(this.name,this,s),r&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){var e,i;"function"==typeof this._$AH?this._$AH.call(null!==(i=null===(e=this.options)||void 0===e?void 0:e.host)&&void 0!==i?i:this.element,t):this._$AH.handleEvent(t)}}class de{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){ie(this,t)}}const ce=Tt.litHtmlPolyfillSupport;null==ce||ce(ee,oe),(null!==(Ht=Tt.litHtmlVersions)&&void 0!==Ht?Ht:Tt.litHtmlVersions=[]).push("2.6.1");
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var ue,pe;class $e extends Pt{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){var t,e;const i=super.createRenderRoot();return null!==(t=(e=this.renderOptions).renderBefore)&&void 0!==t||(e.renderBefore=i.firstChild),i}update(t){const e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=((t,e,i)=>{var s,o;const r=null!==(s=null==i?void 0:i.renderBefore)&&void 0!==s?s:e;let n=r._$litPart$;if(void 0===n){const t=null!==(o=null==i?void 0:i.renderBefore)&&void 0!==o?o:null;r._$litPart$=n=new oe(e.insertBefore(Bt(),t),t,void 0,null!=i?i:{})}return n._$AI(t),n})(e,this.renderRoot,this.renderOptions)}connectedCallback(){var t;super.connectedCallback(),null===(t=this._$Do)||void 0===t||t.setConnected(!0)}disconnectedCallback(){var t;super.disconnectedCallback(),null===(t=this._$Do)||void 0===t||t.setConnected(!1)}render(){return Ft}}$e.finalized=!0,$e._$litElement$=!0,null===(ue=globalThis.litElementHydrateSupport)||void 0===ue||ue.call(globalThis,{LitElement:$e});const ve=globalThis.litElementPolyfillSupport;null==ve||ve({LitElement:$e}),(null!==(pe=globalThis.litElementVersions)&&void 0!==pe?pe:globalThis.litElementVersions=[]).push("3.2.2");const fe=yt`#052c65`,_e=yt`#2b2f32`,ge=yt`#0a3622`,me=yt`#055160`,ye=yt`#664d03`,be=yt`#58151c`,Ae=yt`#495057`,Ee=yt`#495057`,Se=yt`#f8d7da`,we=yt`
  :host {
    color: #222;
    font-family: "Open Sans", Helvetica, Arial, sans-serif;
  }

  a:any-link {
    color: ${rt};
    text-decoration: none;
  }

  button {
    white-space: nowrap;
    font-size: 0.9rem;
    border-radius: 3px;
    border: none;
    padding: 0.4rem 1rem;
    cursor: pointer;
    background-color: ${lt};
    color: ${at};
  }

  button:disabled {
    cursor: default;
  }

  button.primary {
    background-color: ${ht};
    color: ${dt};
  }

  button.success {
    background-color: ${ct};
    color: ${ut};
  }

  button.danger {
    background-color: ${Se};
    color: ${be};
  }

  a:any-link:hover,
  button.text:hover {
    color: ${nt};
    cursor: pointer;
  }

  button.text {
    background: transparent;
    border: none;
    padding: 0;
    color: ${rt};
    font-size: 1rem;
  }

  dl {
    margin-block-start: 0;
    margin-block-end: 0;
  }

  dl > div {
    margin-bottom: 1rem;
  }

  dl > div:last-child {
    margin-bottom: 0;
  }

  dt {
    display: inline;
    font-weight: bold;
  }

  dt:after {
    content: ":";
    margin-right: 0.5em;
  }

  dd {
    display: inline;
    margin: 0;
    line-height: 1.2em;
    font-style: italic;
  }

  dd::after {
    content: ",";
    padding-right: 0.2em;
  }

  dd:last-child::after {
    content: none;
    padding-right: 0;
  }

  form > label {
    font-size: 1rem;
    color: black;
    cursor: pointer;
    display: block;
    font-weight: bold;
    line-height: 1.5;
    margin-bottom: 0;
  }

  form > em {
    display: block;
    padding: 0.5rem 0;
    color: #444;
  }

  input,
  select {
    background-color: #fff;
    font-family: inherit;
    border: 1px solid #ccc;
    color: rgba(0, 0, 0, 0.75);
    font-size: 0.875rem;
    padding: 0.5rem;
  }

  label.required:after {
    content: "*";
    color: red;
  }
`,Ce=yt`
  table {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
  }

  tr {
    border-bottom: solid #eee 1px;
  }

  tr:last-child {
    border-bottom: none;
  }

  tbody > tr:hover {
    background-color: #f7f7f7;
  }

  th,
  td {
    text-align: left;
    padding: 0.5rem 0.25rem;
  }

  th {
    color: #555;
    font-size: 0.9rem;
  }

  a.view-all {
    font-weight: bold;
  }
`,xe=yt`
  .alert {
    position: relative;
    padding: 1rem;
    margin-bottom: 1rem;
    border: 1px solid transparent;
    border-radius: 0.375rem;
  }

  .alert a {
    font-weight: 700;
  }

  .alert-primary {
    color: ${fe};
    background-color: ${yt`#cfe2ff`};
    border-color: ${yt`#9ec5fe`};
  }

  .alert-primary a {
    color: ${fe};
  }

  .alert-secondary {
    color: ${_e};
    background-color: ${yt`#e2e3e5`};
    border-color: ${yt`#c4c8cb`};
  }

  .alert-secondary a {
    color: ${_e};
  }

  .alert-success {
    color: ${ge};
    background-color: ${yt`#d1e7dd`};
    border-color: ${yt`#a3cfbb`};
  }

  .alert-success a {
    color: ${ge};
  }

  .alert-info {
    color: ${me};
    background-color: ${yt`#cff4fc`};
    border-color: ${yt`#9eeaf9`};
  }

  .alert-info a {
    color: ${me};
  }

  .alert-warning {
    color: ${ye};
    background-color: ${yt`#fff3cd`};
    border-color: ${yt`#ffe69c`};
  }

  .alert-warning a {
    color: ${ye};
  }

  .alert-danger {
    color: ${be};
    background-color: ${Se};
    border-color: ${yt`#f1aeb5`};
  }

  .alert-danger a {
    color: ${be};
  }

  .alert-light {
    color: ${Ae};
    background-color: ${yt`#fcfcfd`};
    border-color: ${yt`#e9ecef`};
  }

  .alert-light a {
    color: ${Ae};
  }

  .alert-dark {
    color: ${Ee};
    background-color: ${yt`#ced4da`};
    border-color: ${yt`#adb5bd`};
  }

  .alert-dark a {
    color: ${Ee};
  }
`;export{xe as B,$t as G,t as _,e as a,$e as b,r as c,Qt as d,s as e,Pt as f,we as g,ft as h,yt as i,Ce as j,lt as k,a as l,o,p as r,st as s,Ft as x,Gt as y};
//# sourceMappingURL=chunk-styles-bfd25595.js.map
