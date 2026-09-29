package de.nidal.taskboard.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class TasksPage {

    private final Page page;

    public TasksPage(Page page) {
        this.page = page;
    }

    public TasksPage open() {
        page.navigate("/tasks");
        return this;
    }

    // Kopfbereich

    public Locator title() {
        return page.getByTestId("tasks-title");
    }

    public Locator logoutButton() {
        return page.getByTestId("logout");
    }

    // Neue Aufgabe anlegen (TB-3)

    public Locator newTaskTitleInput() {
        return page.getByTestId("new-task-title");
    }

    public Locator newTaskSubmitButton() {
        return page.getByTestId("new-task-submit");
    }

    public Locator taskErrorMessage() {
        return page.getByTestId("task-error");
    }

    public void addTask(String taskTitle) {
        newTaskTitleInput().fill(taskTitle);
        newTaskSubmitButton().click();
    }

    // Filter und Suche (TB-5)

    public Locator filterStatusSelect() {
        return page.getByTestId("filter-status");
    }

    public Locator searchInput() {
        return page.getByTestId("search-input");
    }

    public Locator emptyMessage() {
        return page.getByTestId("tasks-empty");
    }

    public void filterByStatus(String statusValue) {
        filterStatusSelect().selectOption(statusValue);
    }

    public void searchFor(String text) {
        searchInput().fill(text);
    }

    // Die Liste

    public Locator taskItems() {
        return page.getByTestId("task-item");
    }

    public Locator taskItemByTitle(String taskTitle) {
        return taskItems().filter(new Locator.FilterOptions().setHas(
                page.getByTestId("task-title").getByText(taskTitle, new Locator.GetByTextOptions().setExact(true))
        ));
    }

    public Locator taskTitleOf(Locator taskItem) {
        return taskItem.getByTestId("task-title");
    }

    public Locator taskStatusOf(Locator taskItem) {
        return taskItem.getByTestId("task-status");
    }

    public Locator taskEditButtonOf(Locator taskItem) {
        return taskItem.getByTestId("task-edit");
    }

    public Locator taskDeleteButtonOf(Locator taskItem) {
        return taskItem.getByTestId("task-delete");
    }

    public void changeStatus(Locator taskItem, String statusValue) {
        taskStatusOf(taskItem).selectOption(statusValue);
    }

        // Bearbeitungsmodus (TB-4)
    // Es ist immer höchstens eine Aufgabe im Bearbeitungsmodus. Die Elemente werden deshalb
    // auf der ganzen Seite gesucht, denn im Bearbeitungsmodus fehlt das Titel-Element,
    // über das eine Aufgabe sonst gefunden wird.

    public Locator taskEditInput() {
        return page.getByTestId("task-edit-input");
    }

    public Locator taskSaveButton() {
        return page.getByTestId("task-save");
    }

    public Locator taskCancelEditButton() {
        return page.getByTestId("task-cancel-edit");
    }

    public void editTitle(Locator taskItem, String newTitle) {
        taskEditButtonOf(taskItem).click();
        taskEditInput().fill(newTitle);
        taskSaveButton().click();
    }

    // Sicherheitsabfrage beim Löschen (TB-4)

    public Locator deleteDialog() {
        return page.getByTestId("delete-dialog");
    }

    public Locator deleteConfirmButton() {
        return page.getByTestId("delete-confirm");
    }

    public Locator deleteCancelButton() {
        return page.getByTestId("delete-cancel");
    }

    public void deleteTask(Locator taskItem) {
        taskDeleteButtonOf(taskItem).click();
        deleteConfirmButton().click();
    }
}
