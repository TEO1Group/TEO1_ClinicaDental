import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app and query version', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/version');
    req.flush({ commit: 'dc45780', fechaConstruccion: '2026-10-28T16:22:12Z' });

    expect(app).toBeTruthy();
    expect(app.versionCommit()).toBe('dc45780');
  });

  it('should not show layout before a protected route is active', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/version');
    req.flush(null);

    expect(app.mostrarLayout()).toBe(false);
  });
});
