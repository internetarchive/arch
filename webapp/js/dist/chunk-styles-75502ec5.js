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
 */var n;null===(n=window.HTMLSlotElement)||void 0===n||n.prototype.assignedElements;
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const l=window.ShadowRoot&&(void 0===window.ShadyCSS||window.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,a=Symbol(),h=new Map;class d{constructor(t,e){if(this._$cssResult$=!0,e!==a)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t}get styleSheet(){let t=h.get(this.cssText);return l&&void 0===t&&(h.set(this.cssText,t=new CSSStyleSheet),t.replaceSync(this.cssText)),t}toString(){return this.cssText}}const c=(t,...e)=>{const i=1===t.length?t[0]:e.reduce(((e,i,s)=>e+(t=>{if(!0===t._$cssResult$)return t.cssText;if("number"==typeof t)return t;throw Error("Value passed to 'css' function must be a 'css' function result: "+t+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(i)+t[s+1]),t[0]);return new d(i,a)},u=l?t=>t:t=>t instanceof CSSStyleSheet?(t=>{let e="";for(const i of t.cssRules)e+=i.cssText;return(t=>new d("string"==typeof t?t:t+"",a))(e)})(t):t
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;var p;const $=window.trustedTypes,v=$?$.emptyScript:"",f=window.reactiveElementPolyfillSupport,_={toAttribute(t,e){switch(e){case Boolean:t=t?v:null;break;case Object:case Array:t=null==t?t:JSON.stringify(t)}return t},fromAttribute(t,e){let i=t;switch(e){case Boolean:i=null!==t;break;case Number:i=null===t?null:Number(t);break;case Object:case Array:try{i=JSON.parse(t)}catch(t){i=null}}return i}},g=(t,e)=>e!==t&&(e==e||t==t),m={attribute:!0,type:String,converter:_,reflect:!1,hasChanged:g};class A extends HTMLElement{constructor(){super(),this._$Et=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Ei=null,this.o()}static addInitializer(t){var e;null!==(e=this.l)&&void 0!==e||(this.l=[]),this.l.push(t)}static get observedAttributes(){this.finalize();const t=[];return this.elementProperties.forEach(((e,i)=>{const s=this._$Eh(i,e);void 0!==s&&(this._$Eu.set(s,i),t.push(s))})),t}static createProperty(t,e=m){if(e.state&&(e.attribute=!1),this.finalize(),this.elementProperties.set(t,e),!e.noAccessor&&!this.prototype.hasOwnProperty(t)){const i="symbol"==typeof t?Symbol():"__"+t,s=this.getPropertyDescriptor(t,i,e);void 0!==s&&Object.defineProperty(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){return{get(){return this[e]},set(s){const o=this[t];this[e]=s,this.requestUpdate(t,o,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)||m}static finalize(){if(this.hasOwnProperty("finalized"))return!1;this.finalized=!0;const t=Object.getPrototypeOf(this);if(t.finalize(),this.elementProperties=new Map(t.elementProperties),this._$Eu=new Map,this.hasOwnProperty("properties")){const t=this.properties,e=[...Object.getOwnPropertyNames(t),...Object.getOwnPropertySymbols(t)];for(const i of e)this.createProperty(i,t[i])}return this.elementStyles=this.finalizeStyles(this.styles),!0}static finalizeStyles(t){const e=[];if(Array.isArray(t)){const i=new Set(t.flat(1/0).reverse());for(const t of i)e.unshift(u(t))}else void 0!==t&&e.push(u(t));return e}static _$Eh(t,e){const i=e.attribute;return!1===i?void 0:"string"==typeof i?i:"string"==typeof t?t.toLowerCase():void 0}o(){var t;this._$Ep=new Promise((t=>this.enableUpdating=t)),this._$AL=new Map,this._$Em(),this.requestUpdate(),null===(t=this.constructor.l)||void 0===t||t.forEach((t=>t(this)))}addController(t){var e,i;(null!==(e=this._$Eg)&&void 0!==e?e:this._$Eg=[]).push(t),void 0!==this.renderRoot&&this.isConnected&&(null===(i=t.hostConnected)||void 0===i||i.call(t))}removeController(t){var e;null===(e=this._$Eg)||void 0===e||e.splice(this._$Eg.indexOf(t)>>>0,1)}_$Em(){this.constructor.elementProperties.forEach(((t,e)=>{this.hasOwnProperty(e)&&(this._$Et.set(e,this[e]),delete this[e])}))}createRenderRoot(){var t;const e=null!==(t=this.shadowRoot)&&void 0!==t?t:this.attachShadow(this.constructor.shadowRootOptions);return((t,e)=>{l?t.adoptedStyleSheets=e.map((t=>t instanceof CSSStyleSheet?t:t.styleSheet)):e.forEach((e=>{const i=document.createElement("style"),s=window.litNonce;void 0!==s&&i.setAttribute("nonce",s),i.textContent=e.cssText,t.appendChild(i)}))})(e,this.constructor.elementStyles),e}connectedCallback(){var t;void 0===this.renderRoot&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),null===(t=this._$Eg)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostConnected)||void 0===e?void 0:e.call(t)}))}enableUpdating(t){}disconnectedCallback(){var t;null===(t=this._$Eg)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostDisconnected)||void 0===e?void 0:e.call(t)}))}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$ES(t,e,i=m){var s,o;const r=this.constructor._$Eh(t,i);if(void 0!==r&&!0===i.reflect){const n=(null!==(o=null===(s=i.converter)||void 0===s?void 0:s.toAttribute)&&void 0!==o?o:_.toAttribute)(e,i.type);this._$Ei=t,null==n?this.removeAttribute(r):this.setAttribute(r,n),this._$Ei=null}}_$AK(t,e){var i,s,o;const r=this.constructor,n=r._$Eu.get(t);if(void 0!==n&&this._$Ei!==n){const t=r.getPropertyOptions(n),l=t.converter,a=null!==(o=null!==(s=null===(i=l)||void 0===i?void 0:i.fromAttribute)&&void 0!==s?s:"function"==typeof l?l:null)&&void 0!==o?o:_.fromAttribute;this._$Ei=n,this[n]=a(e,t.type),this._$Ei=null}}requestUpdate(t,e,i){let s=!0;void 0!==t&&(((i=i||this.constructor.getPropertyOptions(t)).hasChanged||g)(this[t],e)?(this._$AL.has(t)||this._$AL.set(t,e),!0===i.reflect&&this._$Ei!==t&&(void 0===this._$EC&&(this._$EC=new Map),this._$EC.set(t,i))):s=!1),!this.isUpdatePending&&s&&(this._$Ep=this._$E_())}async _$E_(){this.isUpdatePending=!0;try{await this._$Ep}catch(t){Promise.reject(t)}const t=this.scheduleUpdate();return null!=t&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var t;if(!this.isUpdatePending)return;this.hasUpdated,this._$Et&&(this._$Et.forEach(((t,e)=>this[e]=t)),this._$Et=void 0);let e=!1;const i=this._$AL;try{e=this.shouldUpdate(i),e?(this.willUpdate(i),null===(t=this._$Eg)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostUpdate)||void 0===e?void 0:e.call(t)})),this.update(i)):this._$EU()}catch(t){throw e=!1,this._$EU(),t}e&&this._$AE(i)}willUpdate(t){}_$AE(t){var e;null===(e=this._$Eg)||void 0===e||e.forEach((t=>{var e;return null===(e=t.hostUpdated)||void 0===e?void 0:e.call(t)})),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EU(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$Ep}shouldUpdate(t){return!0}update(t){void 0!==this._$EC&&(this._$EC.forEach(((t,e)=>this._$ES(e,this[e],t))),this._$EC=void 0),this._$EU()}updated(t){}firstUpdated(t){}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var y;A.finalized=!0,A.elementProperties=new Map,A.elementStyles=[],A.shadowRootOptions={mode:"open"},null==f||f({ReactiveElement:A}),(null!==(p=globalThis.reactiveElementVersions)&&void 0!==p?p:globalThis.reactiveElementVersions=[]).push("1.3.1");const b=globalThis.trustedTypes,E=b?b.createPolicy("lit-html",{createHTML:t=>t}):void 0,S=`lit$${(Math.random()+"").slice(9)}$`,w="?"+S,C=`<${w}>`,x=document,U=(t="")=>x.createComment(t),P=t=>null===t||"object"!=typeof t&&"function"!=typeof t,k=Array.isArray,H=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,T=/-->/g,O=/>/g,N=/>|[ 	\n\r](?:([^\s"'>=/]+)([ 	\n\r]*=[ 	\n\r]*(?:[^ 	\n\r"'`<>=]|("|')|))|$)/g,R=/'/g,M=/"/g,z=/^(?:script|style|textarea|title)$/i,L=Symbol.for("lit-noChange"),B=Symbol.for("lit-nothing"),D=new WeakMap,j=x.createTreeWalker(x,129,null,!1);class I{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let o=0,r=0;const n=t.length-1,l=this.parts,[a,h]=((t,e)=>{const i=t.length-1,s=[];let o,r=2===e?"<svg>":"",n=H;for(let e=0;e<i;e++){const i=t[e];let l,a,h=-1,d=0;for(;d<i.length&&(n.lastIndex=d,a=n.exec(i),null!==a);)d=n.lastIndex,n===H?"!--"===a[1]?n=T:void 0!==a[1]?n=O:void 0!==a[2]?(z.test(a[2])&&(o=RegExp("</"+a[2],"g")),n=N):void 0!==a[3]&&(n=N):n===N?">"===a[0]?(n=null!=o?o:H,h=-1):void 0===a[1]?h=-2:(h=n.lastIndex-a[2].length,l=a[1],n=void 0===a[3]?N:'"'===a[3]?M:R):n===M||n===R?n=N:n===T||n===O?n=H:(n=N,o=void 0);const c=n===N&&t[e+1].startsWith("/>")?" ":"";r+=n===H?i+C:h>=0?(s.push(l),i.slice(0,h)+"$lit$"+i.slice(h)+S+c):i+S+(-2===h?(s.push(void 0),e):c)}const l=r+(t[i]||"<?>")+(2===e?"</svg>":"");if(!Array.isArray(t)||!t.hasOwnProperty("raw"))throw Error("invalid template strings array");return[void 0!==E?E.createHTML(l):l,s]})(t,e);if(this.el=I.createElement(a,i),j.currentNode=this.el.content,2===e){const t=this.el.content,e=t.firstChild;e.remove(),t.append(...e.childNodes)}for(;null!==(s=j.nextNode())&&l.length<n;){if(1===s.nodeType){if(s.hasAttributes()){const t=[];for(const e of s.getAttributeNames())if(e.endsWith("$lit$")||e.startsWith(S)){const i=h[r++];if(t.push(e),void 0!==i){const t=s.getAttribute(i.toLowerCase()+"$lit$").split(S),e=/([.?@])?(.*)/.exec(i);l.push({type:1,index:o,name:e[2],strings:t,ctor:"."===e[1]?J:"?"===e[1]?G:"@"===e[1]?F:K})}else l.push({type:6,index:o})}for(const e of t)s.removeAttribute(e)}if(z.test(s.tagName)){const t=s.textContent.split(S),e=t.length-1;if(e>0){s.textContent=b?b.emptyScript:"";for(let i=0;i<e;i++)s.append(t[i],U()),j.nextNode(),l.push({type:2,index:++o});s.append(t[e],U())}}}else if(8===s.nodeType)if(s.data===w)l.push({type:2,index:o});else{let t=-1;for(;-1!==(t=s.data.indexOf(S,t+1));)l.push({type:7,index:o}),t+=S.length-1}o++}}static createElement(t,e){const i=x.createElement("template");return i.innerHTML=t,i}}function V(t,e,i=t,s){var o,r,n,l;if(e===L)return e;let a=void 0!==s?null===(o=i._$Cl)||void 0===o?void 0:o[s]:i._$Cu;const h=P(e)?void 0:e._$litDirective$;return(null==a?void 0:a.constructor)!==h&&(null===(r=null==a?void 0:a._$AO)||void 0===r||r.call(a,!1),void 0===h?a=void 0:(a=new h(t),a._$AT(t,i,s)),void 0!==s?(null!==(n=(l=i)._$Cl)&&void 0!==n?n:l._$Cl=[])[s]=a:i._$Cu=a),void 0!==a&&(e=V(t,a._$AS(t,e.values),a,s)),e}class W{constructor(t,e){this.v=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}p(t){var e;const{el:{content:i},parts:s}=this._$AD,o=(null!==(e=null==t?void 0:t.creationScope)&&void 0!==e?e:x).importNode(i,!0);j.currentNode=o;let r=j.nextNode(),n=0,l=0,a=s[0];for(;void 0!==a;){if(n===a.index){let e;2===a.type?e=new q(r,r.nextSibling,this,t):1===a.type?e=new a.ctor(r,a.name,a.strings,this,t):6===a.type&&(e=new Q(r,this,t)),this.v.push(e),a=s[++l]}n!==(null==a?void 0:a.index)&&(r=j.nextNode(),n++)}return o}m(t){let e=0;for(const i of this.v)void 0!==i&&(void 0!==i.strings?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}}class q{constructor(t,e,i,s){var o;this.type=2,this._$AH=B,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cg=null===(o=null==s?void 0:s.isConnected)||void 0===o||o}get _$AU(){var t,e;return null!==(e=null===(t=this._$AM)||void 0===t?void 0:t._$AU)&&void 0!==e?e:this._$Cg}get parentNode(){let t=this._$AA.parentNode;const e=this._$AM;return void 0!==e&&11===t.nodeType&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=V(this,t,e),P(t)?t===B||null==t||""===t?(this._$AH!==B&&this._$AR(),this._$AH=B):t!==this._$AH&&t!==L&&this.$(t):void 0!==t._$litType$?this.T(t):void 0!==t.nodeType?this.k(t):(t=>{var e;return k(t)||"function"==typeof(null===(e=t)||void 0===e?void 0:e[Symbol.iterator])})(t)?this.S(t):this.$(t)}M(t,e=this._$AB){return this._$AA.parentNode.insertBefore(t,e)}k(t){this._$AH!==t&&(this._$AR(),this._$AH=this.M(t))}$(t){this._$AH!==B&&P(this._$AH)?this._$AA.nextSibling.data=t:this.k(x.createTextNode(t)),this._$AH=t}T(t){var e;const{values:i,_$litType$:s}=t,o="number"==typeof s?this._$AC(t):(void 0===s.el&&(s.el=I.createElement(s.h,this.options)),s);if((null===(e=this._$AH)||void 0===e?void 0:e._$AD)===o)this._$AH.m(i);else{const t=new W(o,this),e=t.p(this.options);t.m(i),this.k(e),this._$AH=t}}_$AC(t){let e=D.get(t.strings);return void 0===e&&D.set(t.strings,e=new I(t)),e}S(t){k(this._$AH)||(this._$AH=[],this._$AR());const e=this._$AH;let i,s=0;for(const o of t)s===e.length?e.push(i=new q(this.M(U()),this.M(U()),this,this.options)):i=e[s],i._$AI(o),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){var i;for(null===(i=this._$AP)||void 0===i||i.call(this,!1,!0,e);t&&t!==this._$AB;){const e=t.nextSibling;t.remove(),t=e}}setConnected(t){var e;void 0===this._$AM&&(this._$Cg=t,null===(e=this._$AP)||void 0===e||e.call(this,t))}}class K{constructor(t,e,i,s,o){this.type=1,this._$AH=B,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=o,i.length>2||""!==i[0]||""!==i[1]?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=B}get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}_$AI(t,e=this,i,s){const o=this.strings;let r=!1;if(void 0===o)t=V(this,t,e,0),r=!P(t)||t!==this._$AH&&t!==L,r&&(this._$AH=t);else{const s=t;let n,l;for(t=o[0],n=0;n<o.length-1;n++)l=V(this,s[i+n],e,n),l===L&&(l=this._$AH[n]),r||(r=!P(l)||l!==this._$AH[n]),l===B?t=B:t!==B&&(t+=(null!=l?l:"")+o[n+1]),this._$AH[n]=l}r&&!s&&this.C(t)}C(t){t===B?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,null!=t?t:"")}}class J extends K{constructor(){super(...arguments),this.type=3}C(t){this.element[this.name]=t===B?void 0:t}}const Z=b?b.emptyScript:"";class G extends K{constructor(){super(...arguments),this.type=4}C(t){t&&t!==B?this.element.setAttribute(this.name,Z):this.element.removeAttribute(this.name)}}class F extends K{constructor(t,e,i,s,o){super(t,e,i,s,o),this.type=5}_$AI(t,e=this){var i;if((t=null!==(i=V(this,t,e,0))&&void 0!==i?i:B)===L)return;const s=this._$AH,o=t===B&&s!==B||t.capture!==s.capture||t.once!==s.once||t.passive!==s.passive,r=t!==B&&(s===B||o);o&&this.element.removeEventListener(this.name,this,s),r&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){var e,i;"function"==typeof this._$AH?this._$AH.call(null!==(i=null===(e=this.options)||void 0===e?void 0:e.host)&&void 0!==i?i:this.element,t):this._$AH.handleEvent(t)}}class Q{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){V(this,t)}}const X=window.litHtmlPolyfillSupport;
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var Y,tt;null==X||X(I,q),(null!==(y=globalThis.litHtmlVersions)&&void 0!==y?y:globalThis.litHtmlVersions=[]).push("2.2.2");class et extends A{constructor(){super(...arguments),this.renderOptions={host:this},this._$Dt=void 0}createRenderRoot(){var t,e;const i=super.createRenderRoot();return null!==(t=(e=this.renderOptions).renderBefore)&&void 0!==t||(e.renderBefore=i.firstChild),i}update(t){const e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Dt=((t,e,i)=>{var s,o;const r=null!==(s=null==i?void 0:i.renderBefore)&&void 0!==s?s:e;let n=r._$litPart$;if(void 0===n){const t=null!==(o=null==i?void 0:i.renderBefore)&&void 0!==o?o:null;r._$litPart$=n=new q(e.insertBefore(U(),t),t,void 0,null!=i?i:{})}return n._$AI(t),n})(e,this.renderRoot,this.renderOptions)}connectedCallback(){var t;super.connectedCallback(),null===(t=this._$Dt)||void 0===t||t.setConnected(!0)}disconnectedCallback(){var t;super.disconnectedCallback(),null===(t=this._$Dt)||void 0===t||t.setConnected(!1)}render(){return L}}et.finalized=!0,et._$litElement$=!0,null===(Y=globalThis.litElementHydrateSupport)||void 0===Y||Y.call(globalThis,{LitElement:et});const it=globalThis.litElementPolyfillSupport;null==it||it({LitElement:et}),(null!==(tt=globalThis.litElementVersions)&&void 0!==tt?tt:globalThis.litElementVersions=[]).push("3.2.0");const st=c`#2b74a1`,ot=c`#1b4865`,rt=c`#f0f0f0`,nt=c`#222`,lt=st,at=c`#fff`,ht=c`#1e7b34`,dt=c`#fff`;c`#e3e7e8`;const ct={backgroundColor:lt,border:c`none`,color:at,cursor:c`pointer`,hoverBackgroundColor:ot,hoverColor:at,transition:c`background-color 300ms ease-out`};var ut=c`
  :host {
    /* DataTable action buttons */
    --data-table-action-button-background-color: ${ct.backgroundColor};
    --data-table-action-button-border: ${ct.border};
    --data-table-action-button-color: ${ct.color};
    --data-table-action-button-cursor: ${ct.cursor};
    --data-table-action-button-hover-background-color: ${ct.hoverBackgroundColor};
    --data-table-action-button-hover-color: ${ct.hoverColor};
    --data-table-action-button-transition: ${ct.transition};

    /* DataTable paginator */
    --data-table-paginator-wrapper-font-size: 1rem;
    --data-table-paginator-control-button-background-color: transparent;
    --data-table-paginator-control-button-border: none;
    --data-table-paginator-control-button-color: #348fc6;
    --data-table-paginator-control-button-padding: 0.25rem;
  }

  a:any-link {
    color: ${st};
  }

  a:hover {
    color: ${ot};
  }
`
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;const pt=window,$t=pt.ShadowRoot&&(void 0===pt.ShadyCSS||pt.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,vt=Symbol(),ft=new WeakMap;class _t{constructor(t,e,i){if(this._$cssResult$=!0,i!==vt)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o;const e=this.t;if($t&&void 0===t){const i=void 0!==e&&1===e.length;i&&(t=ft.get(e)),void 0===t&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),i&&ft.set(e,t))}return t}toString(){return this.cssText}}const gt=(t,...e)=>{const i=1===t.length?t[0]:e.reduce(((e,i,s)=>e+(t=>{if(!0===t._$cssResult$)return t.cssText;if("number"==typeof t)return t;throw Error("Value passed to 'css' function must be a 'css' function result: "+t+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(i)+t[s+1]),t[0]);return new _t(i,t,vt)},mt=$t?t=>t:t=>t instanceof CSSStyleSheet?(t=>{let e="";for(const i of t.cssRules)e+=i.cssText;return(t=>new _t("string"==typeof t?t:t+"",void 0,vt))(e)})(t):t
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */;var At;const yt=window,bt=yt.trustedTypes,Et=bt?bt.emptyScript:"",St=yt.reactiveElementPolyfillSupport,wt={toAttribute(t,e){switch(e){case Boolean:t=t?Et:null;break;case Object:case Array:t=null==t?t:JSON.stringify(t)}return t},fromAttribute(t,e){let i=t;switch(e){case Boolean:i=null!==t;break;case Number:i=null===t?null:Number(t);break;case Object:case Array:try{i=JSON.parse(t)}catch(t){i=null}}return i}},Ct=(t,e)=>e!==t&&(e==e||t==t),xt={attribute:!0,type:String,converter:wt,reflect:!1,hasChanged:Ct};class Ut extends HTMLElement{constructor(){super(),this._$Ei=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this._$El=null,this.u()}static addInitializer(t){var e;this.finalize(),(null!==(e=this.h)&&void 0!==e?e:this.h=[]).push(t)}static get observedAttributes(){this.finalize();const t=[];return this.elementProperties.forEach(((e,i)=>{const s=this._$Ep(i,e);void 0!==s&&(this._$Ev.set(s,i),t.push(s))})),t}static createProperty(t,e=xt){if(e.state&&(e.attribute=!1),this.finalize(),this.elementProperties.set(t,e),!e.noAccessor&&!this.prototype.hasOwnProperty(t)){const i="symbol"==typeof t?Symbol():"__"+t,s=this.getPropertyDescriptor(t,i,e);void 0!==s&&Object.defineProperty(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){return{get(){return this[e]},set(s){const o=this[t];this[e]=s,this.requestUpdate(t,o,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)||xt}static finalize(){if(this.hasOwnProperty("finalized"))return!1;this.finalized=!0;const t=Object.getPrototypeOf(this);if(t.finalize(),void 0!==t.h&&(this.h=[...t.h]),this.elementProperties=new Map(t.elementProperties),this._$Ev=new Map,this.hasOwnProperty("properties")){const t=this.properties,e=[...Object.getOwnPropertyNames(t),...Object.getOwnPropertySymbols(t)];for(const i of e)this.createProperty(i,t[i])}return this.elementStyles=this.finalizeStyles(this.styles),!0}static finalizeStyles(t){const e=[];if(Array.isArray(t)){const i=new Set(t.flat(1/0).reverse());for(const t of i)e.unshift(mt(t))}else void 0!==t&&e.push(mt(t));return e}static _$Ep(t,e){const i=e.attribute;return!1===i?void 0:"string"==typeof i?i:"string"==typeof t?t.toLowerCase():void 0}u(){var t;this._$E_=new Promise((t=>this.enableUpdating=t)),this._$AL=new Map,this._$Eg(),this.requestUpdate(),null===(t=this.constructor.h)||void 0===t||t.forEach((t=>t(this)))}addController(t){var e,i;(null!==(e=this._$ES)&&void 0!==e?e:this._$ES=[]).push(t),void 0!==this.renderRoot&&this.isConnected&&(null===(i=t.hostConnected)||void 0===i||i.call(t))}removeController(t){var e;null===(e=this._$ES)||void 0===e||e.splice(this._$ES.indexOf(t)>>>0,1)}_$Eg(){this.constructor.elementProperties.forEach(((t,e)=>{this.hasOwnProperty(e)&&(this._$Ei.set(e,this[e]),delete this[e])}))}createRenderRoot(){var t;const e=null!==(t=this.shadowRoot)&&void 0!==t?t:this.attachShadow(this.constructor.shadowRootOptions);return((t,e)=>{$t?t.adoptedStyleSheets=e.map((t=>t instanceof CSSStyleSheet?t:t.styleSheet)):e.forEach((e=>{const i=document.createElement("style"),s=pt.litNonce;void 0!==s&&i.setAttribute("nonce",s),i.textContent=e.cssText,t.appendChild(i)}))})(e,this.constructor.elementStyles),e}connectedCallback(){var t;void 0===this.renderRoot&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),null===(t=this._$ES)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostConnected)||void 0===e?void 0:e.call(t)}))}enableUpdating(t){}disconnectedCallback(){var t;null===(t=this._$ES)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostDisconnected)||void 0===e?void 0:e.call(t)}))}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$EO(t,e,i=xt){var s;const o=this.constructor._$Ep(t,i);if(void 0!==o&&!0===i.reflect){const r=(void 0!==(null===(s=i.converter)||void 0===s?void 0:s.toAttribute)?i.converter:wt).toAttribute(e,i.type);this._$El=t,null==r?this.removeAttribute(o):this.setAttribute(o,r),this._$El=null}}_$AK(t,e){var i;const s=this.constructor,o=s._$Ev.get(t);if(void 0!==o&&this._$El!==o){const t=s.getPropertyOptions(o),r="function"==typeof t.converter?{fromAttribute:t.converter}:void 0!==(null===(i=t.converter)||void 0===i?void 0:i.fromAttribute)?t.converter:wt;this._$El=o,this[o]=r.fromAttribute(e,t.type),this._$El=null}}requestUpdate(t,e,i){let s=!0;void 0!==t&&(((i=i||this.constructor.getPropertyOptions(t)).hasChanged||Ct)(this[t],e)?(this._$AL.has(t)||this._$AL.set(t,e),!0===i.reflect&&this._$El!==t&&(void 0===this._$EC&&(this._$EC=new Map),this._$EC.set(t,i))):s=!1),!this.isUpdatePending&&s&&(this._$E_=this._$Ej())}async _$Ej(){this.isUpdatePending=!0;try{await this._$E_}catch(t){Promise.reject(t)}const t=this.scheduleUpdate();return null!=t&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var t;if(!this.isUpdatePending)return;this.hasUpdated,this._$Ei&&(this._$Ei.forEach(((t,e)=>this[e]=t)),this._$Ei=void 0);let e=!1;const i=this._$AL;try{e=this.shouldUpdate(i),e?(this.willUpdate(i),null===(t=this._$ES)||void 0===t||t.forEach((t=>{var e;return null===(e=t.hostUpdate)||void 0===e?void 0:e.call(t)})),this.update(i)):this._$Ek()}catch(t){throw e=!1,this._$Ek(),t}e&&this._$AE(i)}willUpdate(t){}_$AE(t){var e;null===(e=this._$ES)||void 0===e||e.forEach((t=>{var e;return null===(e=t.hostUpdated)||void 0===e?void 0:e.call(t)})),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$Ek(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$E_}shouldUpdate(t){return!0}update(t){void 0!==this._$EC&&(this._$EC.forEach(((t,e)=>this._$EO(e,this[e],t))),this._$EC=void 0),this._$Ek()}updated(t){}firstUpdated(t){}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var Pt;Ut.finalized=!0,Ut.elementProperties=new Map,Ut.elementStyles=[],Ut.shadowRootOptions={mode:"open"},null==St||St({ReactiveElement:Ut}),(null!==(At=yt.reactiveElementVersions)&&void 0!==At?At:yt.reactiveElementVersions=[]).push("1.6.1");const kt=window,Ht=kt.trustedTypes,Tt=Ht?Ht.createPolicy("lit-html",{createHTML:t=>t}):void 0,Ot=`lit$${(Math.random()+"").slice(9)}$`,Nt="?"+Ot,Rt=`<${Nt}>`,Mt=document,zt=(t="")=>Mt.createComment(t),Lt=t=>null===t||"object"!=typeof t&&"function"!=typeof t,Bt=Array.isArray,Dt=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,jt=/-->/g,It=/>/g,Vt=RegExp(">|[ \t\n\f\r](?:([^\\s\"'>=/]+)([ \t\n\f\r]*=[ \t\n\f\r]*(?:[^ \t\n\f\r\"'`<>=]|(\"|')|))|$)","g"),Wt=/'/g,qt=/"/g,Kt=/^(?:script|style|textarea|title)$/i,Jt=(t=>(e,...i)=>({_$litType$:t,strings:e,values:i}))(1),Zt=Symbol.for("lit-noChange"),Gt=Symbol.for("lit-nothing"),Ft=new WeakMap,Qt=Mt.createTreeWalker(Mt,129,null,!1),Xt=(t,e)=>{const i=t.length-1,s=[];let o,r=2===e?"<svg>":"",n=Dt;for(let e=0;e<i;e++){const i=t[e];let l,a,h=-1,d=0;for(;d<i.length&&(n.lastIndex=d,a=n.exec(i),null!==a);)d=n.lastIndex,n===Dt?"!--"===a[1]?n=jt:void 0!==a[1]?n=It:void 0!==a[2]?(Kt.test(a[2])&&(o=RegExp("</"+a[2],"g")),n=Vt):void 0!==a[3]&&(n=Vt):n===Vt?">"===a[0]?(n=null!=o?o:Dt,h=-1):void 0===a[1]?h=-2:(h=n.lastIndex-a[2].length,l=a[1],n=void 0===a[3]?Vt:'"'===a[3]?qt:Wt):n===qt||n===Wt?n=Vt:n===jt||n===It?n=Dt:(n=Vt,o=void 0);const c=n===Vt&&t[e+1].startsWith("/>")?" ":"";r+=n===Dt?i+Rt:h>=0?(s.push(l),i.slice(0,h)+"$lit$"+i.slice(h)+Ot+c):i+Ot+(-2===h?(s.push(void 0),e):c)}const l=r+(t[i]||"<?>")+(2===e?"</svg>":"");if(!Array.isArray(t)||!t.hasOwnProperty("raw"))throw Error("invalid template strings array");return[void 0!==Tt?Tt.createHTML(l):l,s]};class Yt{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let o=0,r=0;const n=t.length-1,l=this.parts,[a,h]=Xt(t,e);if(this.el=Yt.createElement(a,i),Qt.currentNode=this.el.content,2===e){const t=this.el.content,e=t.firstChild;e.remove(),t.append(...e.childNodes)}for(;null!==(s=Qt.nextNode())&&l.length<n;){if(1===s.nodeType){if(s.hasAttributes()){const t=[];for(const e of s.getAttributeNames())if(e.endsWith("$lit$")||e.startsWith(Ot)){const i=h[r++];if(t.push(e),void 0!==i){const t=s.getAttribute(i.toLowerCase()+"$lit$").split(Ot),e=/([.?@])?(.*)/.exec(i);l.push({type:1,index:o,name:e[2],strings:t,ctor:"."===e[1]?oe:"?"===e[1]?ne:"@"===e[1]?le:se})}else l.push({type:6,index:o})}for(const e of t)s.removeAttribute(e)}if(Kt.test(s.tagName)){const t=s.textContent.split(Ot),e=t.length-1;if(e>0){s.textContent=Ht?Ht.emptyScript:"";for(let i=0;i<e;i++)s.append(t[i],zt()),Qt.nextNode(),l.push({type:2,index:++o});s.append(t[e],zt())}}}else if(8===s.nodeType)if(s.data===Nt)l.push({type:2,index:o});else{let t=-1;for(;-1!==(t=s.data.indexOf(Ot,t+1));)l.push({type:7,index:o}),t+=Ot.length-1}o++}}static createElement(t,e){const i=Mt.createElement("template");return i.innerHTML=t,i}}function te(t,e,i=t,s){var o,r,n,l;if(e===Zt)return e;let a=void 0!==s?null===(o=i._$Co)||void 0===o?void 0:o[s]:i._$Cl;const h=Lt(e)?void 0:e._$litDirective$;return(null==a?void 0:a.constructor)!==h&&(null===(r=null==a?void 0:a._$AO)||void 0===r||r.call(a,!1),void 0===h?a=void 0:(a=new h(t),a._$AT(t,i,s)),void 0!==s?(null!==(n=(l=i)._$Co)&&void 0!==n?n:l._$Co=[])[s]=a:i._$Cl=a),void 0!==a&&(e=te(t,a._$AS(t,e.values),a,s)),e}class ee{constructor(t,e){this.u=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}v(t){var e;const{el:{content:i},parts:s}=this._$AD,o=(null!==(e=null==t?void 0:t.creationScope)&&void 0!==e?e:Mt).importNode(i,!0);Qt.currentNode=o;let r=Qt.nextNode(),n=0,l=0,a=s[0];for(;void 0!==a;){if(n===a.index){let e;2===a.type?e=new ie(r,r.nextSibling,this,t):1===a.type?e=new a.ctor(r,a.name,a.strings,this,t):6===a.type&&(e=new ae(r,this,t)),this.u.push(e),a=s[++l]}n!==(null==a?void 0:a.index)&&(r=Qt.nextNode(),n++)}return o}p(t){let e=0;for(const i of this.u)void 0!==i&&(void 0!==i.strings?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}}class ie{constructor(t,e,i,s){var o;this.type=2,this._$AH=Gt,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cm=null===(o=null==s?void 0:s.isConnected)||void 0===o||o}get _$AU(){var t,e;return null!==(e=null===(t=this._$AM)||void 0===t?void 0:t._$AU)&&void 0!==e?e:this._$Cm}get parentNode(){let t=this._$AA.parentNode;const e=this._$AM;return void 0!==e&&11===t.nodeType&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=te(this,t,e),Lt(t)?t===Gt||null==t||""===t?(this._$AH!==Gt&&this._$AR(),this._$AH=Gt):t!==this._$AH&&t!==Zt&&this.g(t):void 0!==t._$litType$?this.$(t):void 0!==t.nodeType?this.T(t):(t=>Bt(t)||"function"==typeof(null==t?void 0:t[Symbol.iterator]))(t)?this.k(t):this.g(t)}O(t,e=this._$AB){return this._$AA.parentNode.insertBefore(t,e)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}g(t){this._$AH!==Gt&&Lt(this._$AH)?this._$AA.nextSibling.data=t:this.T(Mt.createTextNode(t)),this._$AH=t}$(t){var e;const{values:i,_$litType$:s}=t,o="number"==typeof s?this._$AC(t):(void 0===s.el&&(s.el=Yt.createElement(s.h,this.options)),s);if((null===(e=this._$AH)||void 0===e?void 0:e._$AD)===o)this._$AH.p(i);else{const t=new ee(o,this),e=t.v(this.options);t.p(i),this.T(e),this._$AH=t}}_$AC(t){let e=Ft.get(t.strings);return void 0===e&&Ft.set(t.strings,e=new Yt(t)),e}k(t){Bt(this._$AH)||(this._$AH=[],this._$AR());const e=this._$AH;let i,s=0;for(const o of t)s===e.length?e.push(i=new ie(this.O(zt()),this.O(zt()),this,this.options)):i=e[s],i._$AI(o),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){var i;for(null===(i=this._$AP)||void 0===i||i.call(this,!1,!0,e);t&&t!==this._$AB;){const e=t.nextSibling;t.remove(),t=e}}setConnected(t){var e;void 0===this._$AM&&(this._$Cm=t,null===(e=this._$AP)||void 0===e||e.call(this,t))}}class se{constructor(t,e,i,s,o){this.type=1,this._$AH=Gt,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=o,i.length>2||""!==i[0]||""!==i[1]?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=Gt}get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}_$AI(t,e=this,i,s){const o=this.strings;let r=!1;if(void 0===o)t=te(this,t,e,0),r=!Lt(t)||t!==this._$AH&&t!==Zt,r&&(this._$AH=t);else{const s=t;let n,l;for(t=o[0],n=0;n<o.length-1;n++)l=te(this,s[i+n],e,n),l===Zt&&(l=this._$AH[n]),r||(r=!Lt(l)||l!==this._$AH[n]),l===Gt?t=Gt:t!==Gt&&(t+=(null!=l?l:"")+o[n+1]),this._$AH[n]=l}r&&!s&&this.j(t)}j(t){t===Gt?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,null!=t?t:"")}}class oe extends se{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===Gt?void 0:t}}const re=Ht?Ht.emptyScript:"";class ne extends se{constructor(){super(...arguments),this.type=4}j(t){t&&t!==Gt?this.element.setAttribute(this.name,re):this.element.removeAttribute(this.name)}}class le extends se{constructor(t,e,i,s,o){super(t,e,i,s,o),this.type=5}_$AI(t,e=this){var i;if((t=null!==(i=te(this,t,e,0))&&void 0!==i?i:Gt)===Zt)return;const s=this._$AH,o=t===Gt&&s!==Gt||t.capture!==s.capture||t.once!==s.once||t.passive!==s.passive,r=t!==Gt&&(s===Gt||o);o&&this.element.removeEventListener(this.name,this,s),r&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){var e,i;"function"==typeof this._$AH?this._$AH.call(null!==(i=null===(e=this.options)||void 0===e?void 0:e.host)&&void 0!==i?i:this.element,t):this._$AH.handleEvent(t)}}class ae{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){te(this,t)}}const he=kt.litHtmlPolyfillSupport;null==he||he(Yt,ie),(null!==(Pt=kt.litHtmlVersions)&&void 0!==Pt?Pt:kt.litHtmlVersions=[]).push("2.6.1");
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var de,ce;class ue extends Ut{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){var t,e;const i=super.createRenderRoot();return null!==(t=(e=this.renderOptions).renderBefore)&&void 0!==t||(e.renderBefore=i.firstChild),i}update(t){const e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=((t,e,i)=>{var s,o;const r=null!==(s=null==i?void 0:i.renderBefore)&&void 0!==s?s:e;let n=r._$litPart$;if(void 0===n){const t=null!==(o=null==i?void 0:i.renderBefore)&&void 0!==o?o:null;r._$litPart$=n=new ie(e.insertBefore(zt(),t),t,void 0,null!=i?i:{})}return n._$AI(t),n})(e,this.renderRoot,this.renderOptions)}connectedCallback(){var t;super.connectedCallback(),null===(t=this._$Do)||void 0===t||t.setConnected(!0)}disconnectedCallback(){var t;super.disconnectedCallback(),null===(t=this._$Do)||void 0===t||t.setConnected(!1)}render(){return Zt}}ue.finalized=!0,ue._$litElement$=!0,null===(de=globalThis.litElementHydrateSupport)||void 0===de||de.call(globalThis,{LitElement:ue});const pe=globalThis.litElementPolyfillSupport;null==pe||pe({LitElement:ue}),(null!==(ce=globalThis.litElementVersions)&&void 0!==ce?ce:globalThis.litElementVersions=[]).push("3.2.2");const $e=gt`#052c65`,ve=gt`#2b2f32`,fe=gt`#0a3622`,_e=gt`#055160`,ge=gt`#664d03`,me=gt`#58151c`,Ae=gt`#495057`,ye=gt`#495057`,be=gt`#f8d7da`,Ee=gt`
  :host {
    color: #222;
    font-family: "Open Sans", Helvetica, Arial, sans-serif;
  }

  a:any-link {
    color: ${st};
    text-decoration: none;
  }

  button {
    white-space: nowrap;
    font-size: 0.9rem;
    border-radius: 3px;
    border: none;
    padding: 0.4rem 1rem;
    cursor: pointer;
    background-color: ${rt};
    color: ${nt};
  }

  button:disabled {
    cursor: default;
  }

  button.primary {
    background-color: ${lt};
    color: ${at};
  }

  button.success {
    background-color: ${ht};
    color: ${dt};
  }

  button.danger {
    background-color: ${be};
    color: ${me};
  }

  a:any-link:hover,
  button.text:hover {
    color: ${ot};
    cursor: pointer;
  }

  button.text {
    background: transparent;
    border: none;
    padding: 0;
    color: ${st};
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
`,Se=gt`
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
`,we=gt`
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
    color: ${$e};
    background-color: ${gt`#cfe2ff`};
    border-color: ${gt`#9ec5fe`};
  }

  .alert-primary a {
    color: ${$e};
  }

  .alert-secondary {
    color: ${ve};
    background-color: ${gt`#e2e3e5`};
    border-color: ${gt`#c4c8cb`};
  }

  .alert-secondary a {
    color: ${ve};
  }

  .alert-success {
    color: ${fe};
    background-color: ${gt`#d1e7dd`};
    border-color: ${gt`#a3cfbb`};
  }

  .alert-success a {
    color: ${fe};
  }

  .alert-info {
    color: ${_e};
    background-color: ${gt`#cff4fc`};
    border-color: ${gt`#9eeaf9`};
  }

  .alert-info a {
    color: ${_e};
  }

  .alert-warning {
    color: ${ge};
    background-color: ${gt`#fff3cd`};
    border-color: ${gt`#ffe69c`};
  }

  .alert-warning a {
    color: ${ge};
  }

  .alert-danger {
    color: ${me};
    background-color: ${be};
    border-color: ${gt`#f1aeb5`};
  }

  .alert-danger a {
    color: ${me};
  }

  .alert-light {
    color: ${Ae};
    background-color: ${gt`#fcfcfd`};
    border-color: ${gt`#e9ecef`};
  }

  .alert-light a {
    color: ${Ae};
  }

  .alert-dark {
    color: ${ye};
    background-color: ${gt`#ced4da`};
    border-color: ${gt`#adb5bd`};
  }

  .alert-dark a {
    color: ${ye};
  }
`;export{we as B,ut as G,t as _,e as a,ue as b,r as c,Gt as d,s as e,Se as f,Ee as g,rt as h,gt as i,o,c as r,et as s,Zt as x,Jt as y};
//# sourceMappingURL=chunk-styles-75502ec5.js.map
