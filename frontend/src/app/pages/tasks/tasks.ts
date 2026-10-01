import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Subject, switchMap } from 'rxjs';
import { AuthService } from '../../auth/auth.service';
import { Task, TaskService, TaskStatus } from '../../tasks/task.service';

@Component({
  selector: 'app-tasks',
  imports: [FormsModule],
  templateUrl: './tasks.html',
})
export class Tasks {
  private auth = inject(AuthService);
  private taskService = inject(TaskService);
  private router = inject(Router);

  readonly statusOptions = [
    { value: 'OPEN', label: 'Offen' },
    { value: 'IN_PROGRESS', label: 'In Arbeit' },
    { value: 'DONE', label: 'Erledigt' },
  ];

  tasks = signal<Task[]>([]);
  newTitle = '';
  error = signal('');

  filterStatus: 'ALL' | TaskStatus = 'ALL';
  search = '';
  // switchMap verwirft veraltete Antworten, wenn schnell hintereinander gefiltert oder getippt wird
  private reload$ = new Subject<void>();

  // Es ist immer höchstens eine Aufgabe im Bearbeitungsmodus.
  editingId = signal<number | null>(null);
  editTitle = '';

  taskToDelete = signal<Task | null>(null);

  constructor() {
    this.reload$
      .pipe(
        switchMap(() =>
          this.taskService.list(
            this.filterStatus === 'ALL' ? undefined : this.filterStatus,
            this.search,
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((tasks) => this.tasks.set(tasks));
    this.loadTasks();
  }

  loadTasks() {
    this.reload$.next();
  }

  addTask() {
    this.error.set('');
    this.taskService.create(this.newTitle).subscribe({
      next: () => {
        // neu laden statt anhängen, damit Filter und Suche auch für die neue Aufgabe gelten
        this.loadTasks();
        this.newTitle = '';
      },
      error: (e) => this.showError(e, 'Aufgabe konnte nicht angelegt werden'),
    });
  }

  startEdit(task: Task) {
    this.error.set('');
    this.editingId.set(task.id);
    this.editTitle = task.title;
  }

  cancelEdit() {
    this.editingId.set(null);
  }

  saveEdit(task: Task) {
    this.error.set('');
    this.taskService.rename(task.id, this.editTitle).subscribe({
      next: (updated) => {
        this.replaceTask(updated);
        this.editingId.set(null);
      },
      error: (e) => this.showError(e, 'Aufgabe konnte nicht geändert werden'),
    });
  }

  changeStatus(task: Task, status: string) {
    this.error.set('');
    this.taskService.changeStatus(task.id, status as TaskStatus).subscribe({
      next: (updated) => this.replaceTask(updated),
      error: (e) => {
        this.showError(e, 'Status konnte nicht geändert werden');
        // Auswahlliste wieder auf den gespeicherten Stand bringen
        this.loadTasks();
      },
    });
  }

  askDelete(task: Task) {
    this.error.set('');
    this.taskToDelete.set(task);
  }

  cancelDelete() {
    this.taskToDelete.set(null);
  }

  confirmDelete() {
    const task = this.taskToDelete();
    if (!task) {
      return;
    }
    this.taskService.delete(task.id).subscribe({
      next: () => {
        this.tasks.update((tasks) => tasks.filter((t) => t.id !== task.id));
        this.taskToDelete.set(null);
      },
      error: (e) => {
        this.taskToDelete.set(null);
        this.showError(e, 'Aufgabe konnte nicht gelöscht werden');
      },
    });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  private replaceTask(updated: Task) {
    this.tasks.update((tasks) => tasks.map((t) => (t.id === updated.id ? updated : t)));
  }

  private showError(e: HttpErrorResponse, fallback: string) {
    this.error.set(e.error?.message ?? fallback);
  }
}
