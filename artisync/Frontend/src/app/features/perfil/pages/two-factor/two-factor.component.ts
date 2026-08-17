import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../seguridad/services/auth.service';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { UserResponse } from '../../../../shared/models/user.model';
import { TwoFactorSetupResponse } from '../../../seguridad/models/auth.model';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-two-factor-setup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './two-factor.component.html'
})
export class TwoFactorSetupComponent implements OnInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);
  private location = inject(Location);

  readonly isLoading = signal<boolean>(false);
  readonly userProfile = signal<UserResponse | null>(null);

  readonly is2faEnabled = signal<boolean>(false);
  readonly isSettingUp2fa = signal<boolean>(false);
  readonly setupData = signal<TwoFactorSetupResponse | null>(null);
  readonly qrCodeImage = signal<string>('');

  twoFactorForm = this.fb.group({
    codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
  });

  disableTwoFactorForm = this.fb.group({
    codigo: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadUserProfile();
  }

  goBack(): void {
    this.location.back();
  }

  loadUserProfile(): void {
    this.isLoading.set(true);
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.userProfile.set(user);
        this.is2faEnabled.set(user.dosFactoresHabilitado);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.toastService.error('No se pudo cargar el perfil');
      }
    });
  }

  start2faSetup(): void {
    this.isLoading.set(true);
    this.authService.setup2fa().subscribe({
      next: async (data) => {
        this.setupData.set(data);
        this.isSettingUp2fa.set(true);
        
        try {
          const qrDataUrl = await QRCode.toDataURL(data.otpauthUri, { width: 250, margin: 2 });
          this.qrCodeImage.set(qrDataUrl);
        } catch (err) {
          console.error('Error generando QR', err);
          this.toastService.error('No se pudo generar el código QR visual');
        }

        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  cancel2faSetup(): void {
    this.isSettingUp2fa.set(false);
    this.setupData.set(null);
    this.qrCodeImage.set('');
    this.twoFactorForm.reset();
  }

  confirm2fa(): void {
    if (this.twoFactorForm.invalid) {
      this.toastService.error('Ingresa un código válido');
      return;
    }
    
    const codigo = this.twoFactorForm.value.codigo;
    if (!codigo) return;

    this.isLoading.set(true);
    this.authService.confirm2fa({ codigo }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.toastService.success(res.mensaje || res.message || '2FA activado con éxito');
        this.is2faEnabled.set(true);
        this.isSettingUp2fa.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  disable2fa(): void {
    if (this.disableTwoFactorForm.invalid) {
      this.toastService.error('Ingresa un código válido para desactivar 2FA');
      return;
    }

    const codigo = this.disableTwoFactorForm.value.codigo;
    if (!codigo) return;

    this.isLoading.set(true);
    this.authService.disable2fa({ codigo }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.toastService.success(res.mensaje || res.message || '2FA desactivado con éxito');
        this.is2faEnabled.set(false);
        this.setupData.set(null);
        this.qrCodeImage.set('');
        this.disableTwoFactorForm.reset();
      },
      error: () => this.isLoading.set(false)
    });
  }
}
