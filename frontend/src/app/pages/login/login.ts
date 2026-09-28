import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  error = signal('');
  info = inject(ActivatedRoute).snapshot.queryParamMap.has('registered')
    ? 'Registrierung erfolgreich. Bitte melde dich an.'
    : '';

  submit() {
    this.error.set('');
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/tasks']),
      error: (e) => this.error.set(e.error?.message ?? 'Anmeldung fehlgeschlagen'),
    });
  }
}