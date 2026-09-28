import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  error = signal('');

  submit() {
    this.error.set('');
    this.auth.register(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: 1 } }),
      error: (e) => this.error.set(e.error?.message ?? 'Registrierung fehlgeschlagen'),
    });
  }
}