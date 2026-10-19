import { AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnDestroy, Output, ViewChild, inject } from '@angular/core';

declare global {
  interface Window { google?: any; }
}

const GIS_SRC = 'https://accounts.google.com/gsi/client';

/**
 * Renders Google's official "Sign in with Google" button (Google Identity Services)
 * and emits the ID token ("credential") the backend verifies.
 */
@Component({
  selector: 'app-google-button',
  standalone: true,
  template: `<div #host class="gis-host"></div>
             @if (loadError) { <p class="gis-error">Google sign-in could not be loaded. Use your email and password instead.</p> }`,
  styles: [`
    .gis-host { display: flex; justify-content: center; min-height: 44px; }
    .gis-error { margin: 8px 0 0; font-size: 13.5px; color: #3a4763; text-align: center; }
  `]
})
export class GoogleButtonComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) clientId = '';
  @Input() text: 'signin_with' | 'signup_with' | 'continue_with' = 'continue_with';
  @Output() credential = new EventEmitter<string>();
  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;

  loadError = false;
  private zone = inject(NgZone);
  private destroyed = false;

  ngAfterViewInit(): void {
    this.loadScript()
      .then(() => this.render())
      .catch(() => (this.loadError = true));
  }

  ngOnDestroy(): void {
    this.destroyed = true;
  }

  private loadScript(): Promise<void> {
    if (window.google?.accounts?.id) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>(`script[src="${GIS_SRC}"]`);
      const script = existing ?? document.createElement('script');
      script.addEventListener('load', () => resolve());
      script.addEventListener('error', () => reject());
      if (!existing) {
        script.src = GIS_SRC;
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);
      }
    });
  }

  private render(): void {
    if (this.destroyed || !window.google?.accounts?.id) {
      return;
    }
    window.google.accounts.id.initialize({
      client_id: this.clientId,
      callback: (response: { credential: string }) =>
        this.zone.run(() => this.credential.emit(response.credential)),
      ux_mode: 'popup'
    });
    window.google.accounts.id.renderButton(this.host.nativeElement, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: this.text,
      shape: 'rectangular',
      width: Math.min(this.host.nativeElement.clientWidth || 380, 400)
    });
  }
}
