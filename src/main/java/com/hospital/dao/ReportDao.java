package com.hospital.dao;

import com.hospital.conexion.Conexion;
import com.hospital.model.Report;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cesar31
 */
public class ReportDao {

    private Connection transaction;

    public ReportDao() {
    }

    public ReportDao(Connection transaction) {
        this.transaction = transaction;
    }

    /**
     * Metodo para insertar un informe en la base de datos
     *
     * @param r
     */
    public void insertReport(Report r) {
        String query = "INSERT INTO REPORTS(report_id, patient_id, doctor_id, report, date, time) VALUES(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, r.getReportId());
            //pst.setInt(2, r.getAppointmentId());
            pst.setInt(2, r.getPatientId());
            pst.setString(3, r.getDoctorId());
            pst.setString(4, r.getReport());
            pst.setDate(5, r.getDate());
            pst.setTime(6, r.getTime());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Metodo para obtener un listado de los informes a la base de
     *
     * @param patientId
     * @return
     */
    public List<Report> getReportsByPatient(int patientId) {
        List<Report> reports = new ArrayList<>();
        //String query = "SELECT * FROM REPORTS WHERE patient_id = ? ORDER BY date";
        String query = "SELECT r.*, p.name AS patient, d.name AS doctor FROM REPORTS r INNER JOIN PATIENTS p ON r.patient_id = p.patient_id INNER JOIN DOCTORS d ON r.doctor_id = d.doctor_id "
                + "WHERE r.patient_id = ? ORDER BY date, time";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, patientId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    reports.add(new Report(rs));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return reports;
    }

    /**
     * Metodo para insertar nuevo reporte y actualizar el estado de una cita
     *
     * @param r
     */
    public void createReport(Report r) {
        int id = 0;
        String query = "INSERT INTO REPORTS(appointment_id, patient_id, doctor_id, report, date, time) VALUES(?, ?, ?, ?, ?, ?)";
        String queryIncome = "INSERT INTO INCOMES(report_id, income) VALUES(?, (SELECT price_consultation FROM SPECIALTIES WHERE specialty_id = ? LIMIT 1))";
        String queryUpdate = "UPDATE APPOINTMENTS SET status = ? WHERE appointment_id = ?";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            this.transaction.setAutoCommit(false);
            pst = this.transaction.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            pst.setInt(1, r.getAppointmentId());
            pst.setInt(2, r.getPatientId());
            pst.setString(3, r.getDoctorId());
            pst.setString(4, r.getReport());
            pst.setDate(5, r.getDate());
            pst.setTime(6, r.getTime());
            pst.executeUpdate();

            rs = pst.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }

            pst = this.transaction.prepareStatement(queryIncome);
            pst.setInt(1, id);
            pst.setInt(2, r.getSpecialtyId());
            pst.executeUpdate();
            
            pst = this.transaction.prepareStatement(queryUpdate);
            pst.setBoolean(1, true);
            pst.setInt(2, r.getAppointmentId());
            pst.executeUpdate();
            this.transaction.commit();
        } catch (SQLException ex) {
            try {
                ex.printStackTrace(System.out);
                this.transaction.rollback();
            } catch (SQLException ex1) {
                ex1.printStackTrace(System.out);
            }
        } finally {
            Conexion.close(rs);
            Conexion.close(pst);
        }
    }
}
