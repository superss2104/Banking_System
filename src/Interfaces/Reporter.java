package Interfaces;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import model.Transaction;

import java.time.LocalDate;

public interface Reporter {
    String getReportId();
    void setReportId(String reportId);
    StringProperty reportIdProperty();

    String getGeneratedBy();
    void setGeneratedBy(String generatedBy);
    StringProperty generatedByProperty();

    String getTitle();
    void setTitle(String title);
    StringProperty titleProperty();

    LocalDate getStartDate();
    void setStartDate(LocalDate startDate);
    ObjectProperty<LocalDate> startDateProperty();

    LocalDate getEndDate();
    void setEndDate(LocalDate endDate);
    ObjectProperty<LocalDate> endDateProperty();

    String getContent();
    void setContent(String content);
    StringProperty contentProperty();

    ObservableList<Transaction> getTransactions();
    void setTransactions(ObservableList<Transaction> transactions);
    void addTransaction(Transaction transaction);

    String getDateRange();
}
