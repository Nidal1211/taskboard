import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  register(email: string, password: string) {
    return this.http.post('/api/auth/register', { email, password });
  }

  login(email: string, password: string) {
    return this.http
      .post<{ token: string }>('/api/auth/login', { email, password })
      .pipe(tap((response) => localStorage.setItem('token', response.token)));
  }

  logout() {
    localStorage.removeItem('token');
  }

  isLoggedIn() {
    return localStorage.getItem('token') !== null;
  }
}