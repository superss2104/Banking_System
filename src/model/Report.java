package model;

import Interfaces.Reporter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Report implements Reporter {
    private final StringProperty title = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>();
    private final StringProperty content = new SimpleStringProperty();
    private final StringProperty reportId = new SimpleStringProperty();
    private final StringProperty generatedBy = new SimpleStringProperty();
    private ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    public Report() {
        this("", LocalDate.now(), LocalDate.now());
    }

    public Report(String title, LocalDate startDate, LocalDate endDate) {
        this.title.set(title);
        this.startDate.set(startDate);
        this.endDate.set(endDate);
        this.reportId.set(generateReportId());
    }

    public Report(String reportId, String title, LocalDate startDate, LocalDate endDate,
                  String content, String generatedBy) {
        this.reportId.set(reportId);
        this.title.set(title);
        this.startDate.set(startDate);
        this.endDate.set(endDate);
        this.content.set(content);
        this.generatedBy.set(generatedBy);
    }

    private String generateReportId() {
        return "RPT" + System.currentTimeMillis();
    }

    public String getReportId() {
        return reportId.get();
    }

    public void setReportId(String reportId) {
        this.reportId.set(reportId);
    }

    public StringProperty reportIdProperty() {
        return reportId;
    }

    public String getGeneratedBy() {
        return generatedBy.get();
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy.set(generatedBy);
    }

    public StringProperty generatedByProperty() {
        return generatedBy;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate.get();
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate.set(startDate);
    }

    public ObjectProperty<LocalDate> startDateProperty() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate.get();
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate.set(endDate);
    }

    public ObjectProperty<LocalDate> endDateProperty() {
        return endDate;
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String content) {
        this.content.set(content);
    }

    public StringProperty contentProperty() {
        return content;
    }

    public ObservableList<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(ObservableList<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    public String getDateRange() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return getStartDate().format(formatter) + " to " + getEndDate().format(formatter);
    }

    @Override
    public String toString() {
        return getTitle() + " (" + getDateRange() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Report report = (Report) o;

        return getReportId().equals(report.getReportId());
    }

    @Override
    public int hashCode() {
        return getReportId().hashCode();
    }
}
