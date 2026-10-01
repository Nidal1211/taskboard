import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE';

export interface Task {
  id: number;
  title: string;
  status: TaskStatus;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);

  list(status?: TaskStatus, search?: string) {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<Task[]>('/api/tasks', { params });
  }

  create(title: string) {
    return this.http.post<Task>('/api/tasks', { title });
  }

  rename(id: number, title: string) {
    return this.http.put<Task>(`/api/tasks/${id}`, { title });
  }

  changeStatus(id: number, status: TaskStatus) {
    return this.http.patch<Task>(`/api/tasks/${id}/status`, { status });
  }

  delete(id: number) {
    return this.http.delete<void>(`/api/tasks/${id}`);
  }
}
