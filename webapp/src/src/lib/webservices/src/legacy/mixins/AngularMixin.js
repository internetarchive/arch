export default (cls) =>
  class extends cls {
    setReadyHandler(observedAttributes, onReady) {
      this.onReady = onReady;

      // Collect the set of attribute names with values that have yet to be
      // interpolated by Angular.
      this.attrsPendingInterpolation = new Set(
        observedAttributes.filter((name) =>
          (this.getAttribute(name) || "").includes("{{")
        )
      );

      // Invoke onReady() immediately if no interpolation required.
      if (this.attrsPendingInterpolation.size === 0) {
        this.onReady();
      }
    }

    // eslint-disable-next-line no-unused-vars
    attributeChangedCallback(name, oldValue, newValue) {
      /* Handle attribute values pending Angular interpolation and return
       a bool indicating whether event was handled.
     */
      if (!this.attrsPendingInterpolation) {
        // connectedCallback() hasn't been invoked yet.
        return true;
      }
      const numPendingAttrs = this.attrsPendingInterpolation.size;
      if (numPendingAttrs > 0) {
        if (this.attrsPendingInterpolation.has(name)) {
          this.attrsPendingInterpolation.delete(name);
          if (numPendingAttrs === 1) {
            // That was the last pending attribute, so fire onReady().
            this.onReady();
          }
        }
        return true;
      }
      return false;
    }

    emit(signal, data) {
      /* Angular 1 only supports listening for built-in (i.e. non-custom)
         events, so we'll decided to use "submit" events for passing messages
         up to Angular.

         You can use the ng-submit attribute to indicate Angular code to execute
         when this event is triggered, for example:

           <custom-element ng-submit='angularFunc($event.detail.data)'>
           </custom-element>
     */
      this.dispatchEvent(
        new CustomEvent("submit", {
          detail: { signal, data },
          bubbles: true,
        })
      );
    }

    emitError(error) {
      this.emit("Error", error);
    }

    emitResponseError(response, method, data) {
      /* Emit an error with a error object compatible with the
       existing Angular error dialog code.
     */
      this.emitError({
        status: response.status,
        statusText: response.statusText,
        headers: () => {
          response.headers.get("date");
        },
        config: {
          url: response.url,
          method: method || "n/a",
          data: data || "n/a",
        },
      });
    }

    addSignalHandler(signalName, handler, delayMs = 250) {
      /* Emit a "WebComponent:subscribe" signal with a { signalName, handler } payload
         that we can listen for in Angular to register a callback for Angular-native signals.
         delayMs indicates a waiting period before calling emit, which is required if
         this function is called within the connectedCallback() while Angular is still
         digesting.
       */
      setTimeout(
        () => this.emit("WebComponent:subscribe", { signalName, handler }),
        delayMs
      );
    }
  };
