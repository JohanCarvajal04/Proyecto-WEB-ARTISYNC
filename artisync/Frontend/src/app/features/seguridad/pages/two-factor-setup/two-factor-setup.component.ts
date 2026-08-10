import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import QRCode from 'qrcode';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TwoFactorSetupResponse } from '../../models/auth.model';

@Component({
  selector: 'app-two-factor-setup',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './two-factor-setup.component.html'
})
export class TwoFactorSetupComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  readonly isLoading = signal<boolean>(false);
  readonly setupData = signal<TwoFactorSetupResponse | null>(null);
  readonly qrDataUrl = signal<string>('');
  readonly paso = signal<'INICIO' | 'QR' | 'CONFIRMADO'>('INICIO');

  form: FormGroup = this.fb.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]]
  });

  ngOnInit(): void {
    // Verificar si ya está logueado
    if (!this.authService.isLoggedIn()) {
      this.toastService.warning('Debes iniciar sesión para configurar 2FA');
      this.router.navigate(['/auth/login']);
    }
  }

  iniciarSetup(): void {
    this.isLoading.set(true);
    this.authService.setup2fa().subscribe({
      next: (data) => {
        this.setupData.set(data);
        this.isLoading.set(false);
        this.paso.set('QR');

        QRCode.toDataURL(data.otpauthUri, { width: 220, margin: 2, color: { dark: '#38bdf8', light: '#1e293b' } })
          .then(url => this.qrDataUrl.set(url))
          .catch(err => console.error('Error generando QR', err));
      },
      error: () => this.isLoading.set(false)
    });
  }

  confirmar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { codigo } = this.form.getRawValue();

    this.authService.confirm2fa({ codigo }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.paso.set('CONFIRMADO');
        this.toastService.success('¡Autenticación de dos factores activada!');
      },
      error: () => this.isLoading.set(false)
    });
  }

  copiarCodigos(): void {
    const codigos = this.setupData()?.codigosRespaldo.join('\n') || '';
    navigator.clipboard.writeText(codigos);
    this.toastService.info('Códigos copiados al portapapeles');
  }

  finalizar(): void {
    this.router.navigateByUrl(this.authService.homeRoute());
  }
}
