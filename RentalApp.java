import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/**
 * RentalApp.java
 * MySQL-based Rental Property Management System : All the classes are included in this file only .
 *
 * Features: 
 *  - Authentication (signup/login) with roles: admin, owner, tenant
 *  - Role-based access and filtering
 *  - Validation (phone, rent > 0, date format yyyy-MM-dd)
 *  - Update & Delete for entities
 *  - Search filters (location, rent range, owner id)
 *  - Payment reports: monthly summary & unpaid rent alerts
 *  - view ,update ,delete ,update property
 *  - view ,update,delete ,update owner 
 *  - view ,update,delete ,update leases,teneats and payments .
 * 
 */
public class RentalApp extends JFrame {
    private DBHelper db;

    // Authentication / current user
    private Integer currentUserId = null;
    private String currentUserRole = null; 
    private Integer currentUserRefId = null; 

    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Color bgColor = new Color(240, 250, 255);
    private final Color panelColor = new Color(255, 255, 255);

    private JTabbedPane tabs;

    public RentalApp() {
        super("🏠 Rental Property Management System");

        // --- Initializing database connection ---
         db = new DBHelper(
    "jdbc:mysql://localhost:3306/rental_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
    "root",
        "mus22834812!!!"
);


        db.createTablesIfNotExist();

        // --- Authentication first ---
        showAuthDialog();

        // --- Window setup ---
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(bgColor);
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("Rental Property Management System", JLabel.CENTER);
        header.setFont(titleFont);
        header.setOpaque(true);
        header.setBackground(new Color(0, 123, 255));
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        main.add(header, BorderLayout.NORTH);

        // --- Tabs for modules ---
        tabs = new JTabbedPane();
        tabs.setFont(labelFont);
        tabs.addTab("Properties", createPropertiesPanel());
        tabs.addTab("Owners", createOwnersPanel());
        tabs.addTab("Tenants", createTenantsPanel());
        tabs.addTab("Leases", createLeasesPanel());
        tabs.addTab("Payments", createPaymentsPanel());
        tabs.addTab("Search", createSearchPanel());
        tabs.addTab("Reports", createReportsPanel());
        main.add(tabs, BorderLayout.CENTER);

        // Role-based tab visibility
        applyRoleAccess();

        // --- Exit Button ---
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(bgColor);
        JButton exitBtn = new JButton("Exit");
        exitBtn.addActionListener(e -> {
            db.close();
            System.exit(0);
        });
        bottom.add(exitBtn);
        main.add(bottom, BorderLayout.SOUTH);

        setContentPane(main);
        setVisible(true);
    }

    // ---------------- AUTH ----------------
    private void showAuthDialog() {
        JDialog dlg = new JDialog(this, "Login / Signup", true);
        dlg.setSize(420, 320);
        dlg.setLocationRelativeTo(null);
        dlg.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.add(new JLabel("<html><b>Welcome — Login or Signup</b></html>"));
        dlg.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel userL = new JLabel("Username:"); c.gridx=0;c.gridy=0; center.add(userL,c);
        JTextField userF = new JTextField(18); c.gridx=1;center.add(userF,c);
        JLabel passL = new JLabel("Password:"); c.gridx=0;c.gridy=1; center.add(passL,c);
        JPasswordField passF = new JPasswordField(18); c.gridx=1;center.add(passF,c);

        JLabel roleL = new JLabel("Role:"); c.gridx=0;c.gridy=2; center.add(roleL,c);
        String[] roles = {"admin","owner","tenant"};
        JComboBox<String> roleBox = new JComboBox<>(roles); c.gridx=1;center.add(roleBox,c);

        // for owner/tenant, a reference id field linking user account to owner/tenant record
        JLabel refL = new JLabel("Ref ID (owner/tenant id) optional:"); c.gridx=0;c.gridy=3; center.add(refL,c);
        JTextField refF = new JTextField(10); c.gridx=1; center.add(refF,c);

        center.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        dlg.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton loginB = new JButton("Login");
        JButton signupB = new JButton("Signup");
        bottom.add(loginB);
        bottom.add(signupB);
        dlg.add(bottom, BorderLayout.SOUTH);

        loginB.addActionListener(ev -> {
            String u = userF.getText().trim();
            String p = new String(passF.getPassword());
            if(u.isEmpty()||p.isEmpty()){ JOptionPane.showMessageDialog(dlg,"Enter username & password"); return; }
            try {
                Integer uid = db.authenticate(u, p);
                if(uid==null){ JOptionPane.showMessageDialog(dlg,"Login failed"); return; }
                // get role info
                UserInfo info = db.getUserInfoById(uid);
                if(info==null){ JOptionPane.showMessageDialog(dlg,"Login failed - user info"); return; }
                this.currentUserId = uid;
                this.currentUserRole = info.role;
                this.currentUserRefId = info.refId;
                JOptionPane.showMessageDialog(dlg,"Logged in as "+info.role+" ("+u+")");
                dlg.dispose();
            } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(dlg,"Error: "+ex.getMessage()); }
        });

        signupB.addActionListener(ev -> {
            String u = userF.getText().trim();
            String p = new String(passF.getPassword());
            String role = (String)roleBox.getSelectedItem();
            Integer refId = null;
            if(!refF.getText().trim().isEmpty()){
                try{ refId = Integer.parseInt(refF.getText().trim()); }catch(Exception ex){ JOptionPane.showMessageDialog(dlg,"Ref ID must be numeric"); return; }
            }
            if(u.isEmpty()||p.isEmpty()){ JOptionPane.showMessageDialog(dlg,"Enter username & password"); return; }
            try {
                boolean ok = db.createUser(u,p,role,refId);
                if(!ok){ JOptionPane.showMessageDialog(dlg,"Signup failed (maybe username exists)"); return; }
                JOptionPane.showMessageDialog(dlg,"Signup successful. Please login.");
            } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(dlg,"Error: "+ex.getMessage()); }
        });

        dlg.setVisible(true);
    }

    // ---------------- PANEL CREATION ----------------
    private JPanel createPropertiesPanel() {
        JPanel panel = makePanelWithBorder("Manage Properties");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(panelColor);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameL = new JLabel("Property Name:"); c.gridx=0;c.gridy=0;form.add(nameL,c);
        JTextField nameF = new JTextField(15); c.gridx=1;form.add(nameF,c);
        JLabel locL = new JLabel("Location:"); c.gridx=0;c.gridy=1;form.add(locL,c);
        JTextField locF = new JTextField(15); c.gridx=1;form.add(locF,c);
        JLabel rentL = new JLabel("Rent:"); c.gridx=0;c.gridy=2;form.add(rentL,c);
        JTextField rentF = new JTextField(15); c.gridx=1;form.add(rentF,c);
        JLabel ownerL = new JLabel("Owner ID:"); c.gridx=0;c.gridy=3;form.add(ownerL,c);
        JTextField ownerF = new JTextField(15); c.gridx=1;form.add(ownerF,c);

        JButton addBtn = new JButton("Add Property");
        JButton viewBtn = new JButton("View Properties");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JPanel btns = new JPanel(); btns.add(addBtn); btns.add(viewBtn); btns.add(updateBtn); btns.add(deleteBtn);
        c.gridx=0;c.gridy=4;c.gridwidth=2;form.add(btns,c);

        String[] cols = {"ID","Name","Location","Rent","Owner ID"};
        DefaultTableModel model = new DefaultTableModel(cols,0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);

        // populate form from selected row when clicked
        table.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                int r = table.getSelectedRow();
                if(r>=0){
                    nameF.setText(model.getValueAt(r,1)+"");
                    locF.setText(model.getValueAt(r,2)+"");
                    rentF.setText(model.getValueAt(r,3)+"");
                    ownerF.setText(model.getValueAt(r,4)+"");
                }
            }
        });

        addBtn.addActionListener(e -> {
            try {
                String name = nameF.getText().trim();
                String loc = locF.getText().trim();
                double rent = parsePositiveDouble(rentF.getText().trim(), "Rent must be a positive number");
                String ownerId = ownerF.getText().trim().isEmpty() ? null : ownerF.getText().trim();
                if(!isValidString(name)) throw new Exception("Property name required");
                if(!isValidLocation(loc)) throw new Exception("Location required");
                if(ownerId!=null && !isNumeric(ownerId)) throw new Exception("Owner ID must be numeric or empty");
                db.insertProperty(name, loc, rent, ownerId);
                JOptionPane.showMessageDialog(this, "✅ Property added successfully!");
                clearFields(nameF, locF, rentF, ownerF);
                // refresh depending on role
                if("owner".equals(currentUserRole)) refreshTable(model, db.getPropertiesForOwner(currentUserRefId));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        viewBtn.addActionListener(e -> {
            if("owner".equals(currentUserRole)){
                refreshTable(model, db.getPropertiesForOwner(currentUserRefId));
            } else {
                refreshTable(model, db.getAll("properties"));
            }
        });

        updateBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select a row to update"); return; }
            try {
                int id = Integer.parseInt(model.getValueAt(r,0).toString());
                String name = nameF.getText().trim();
                String loc = locF.getText().trim();
                double rent = parsePositiveDouble(rentF.getText().trim(), "Rent must be a positive number");
                String ownerId = ownerF.getText().trim().isEmpty() ? null : ownerF.getText().trim();
                if(!isValidString(name)) throw new Exception("Property name required");
                db.updateProperty(id, name, loc, rent, ownerId);
                JOptionPane.showMessageDialog(this,"Updated!");
                viewBtn.doClick();
            } catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        deleteBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select a row to delete"); return; }
            int confirm = JOptionPane.showConfirmDialog(this,"Delete this property?","Confirm",JOptionPane.YES_NO_OPTION);
            if(confirm!=JOptionPane.YES_OPTION) return;
            int id = Integer.parseInt(model.getValueAt(r,0).toString());
            db.deleteById("properties", id);
            JOptionPane.showMessageDialog(this,"Deleted!");
            viewBtn.doClick();
        });

        panel.setLayout(new BorderLayout());
        panel.add(form, BorderLayout.WEST);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOwnersPanel() {
        JPanel panel = makePanelWithBorder("Manage Owners");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(panelColor);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);

        JLabel nL = new JLabel("Owner Name:"); c.gridx=0;c.gridy=0;form.add(nL,c);
        JTextField nF = new JTextField(15); c.gridx=1;form.add(nF,c);
        JLabel pL = new JLabel("Phone:"); c.gridx=0;c.gridy=1;form.add(pL,c);
        JTextField pF = new JTextField(15); c.gridx=1;form.add(pF,c);
        JButton add = new JButton("Add Owner"); JButton view = new JButton("View Owners");
        JButton update = new JButton("Update Selected"); JButton delete = new JButton("Delete Selected");
        JPanel btns = new JPanel(); btns.add(add); btns.add(view); btns.add(update); btns.add(delete);
        c.gridx=0;c.gridy=2;c.gridwidth=2;form.add(btns,c);

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Name","Phone"},0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);

        table.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                int r = table.getSelectedRow();
                if(r>=0){
                    nF.setText(model.getValueAt(r,1)+"");
                    pF.setText(model.getValueAt(r,2)+"");
                }
            }
        });

        add.addActionListener(e -> {
            try{
                String name = nF.getText().trim();
                String phone = pF.getText().trim();
                if(!isValidString(name)) throw new Exception("Owner name required");
                if(!isValidPhone(phone)) throw new Exception("Invalid phone number");
                db.insertOwner(name, phone);
                JOptionPane.showMessageDialog(this,"Owner added successfully!");
                clearFields(nF,pF);
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        view.addActionListener(e -> {
            // owners visible to admin only; but owners can view themselves (owner role)
            if("owner".equals(currentUserRole)){
                // show only this owner
                if(currentUserRefId!=null) refreshTable(model, db.getOwnerById(currentUserRefId));
                else refreshTable(model, db.getAll("owners"));
            } else {
                refreshTable(model, db.getAll("owners"));
            }
        });

        update.addActionListener(e -> {
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select owner to update"); return; }
            try{
                int id = Integer.parseInt(model.getValueAt(r,0).toString());
                String name = nF.getText().trim();
                String phone = pF.getText().trim();
                if(!isValidString(name)) throw new Exception("Owner name required");
                if(!isValidPhone(phone)) throw new Exception("Invalid phone number");
                db.updateOwner(id, name, phone);
                JOptionPane.showMessageDialog(this,"Updated!");
                view.doClick();
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        delete.addActionListener(e -> {
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select owner to delete"); return; }
            int id = Integer.parseInt(model.getValueAt(r,0).toString());
            int conf = JOptionPane.showConfirmDialog(this,"Delete owner?","Confirm",JOptionPane.YES_NO_OPTION);
            if(conf!=JOptionPane.YES_OPTION) return;
            db.deleteById("owners", id);
            JOptionPane.showMessageDialog(this,"Deleted");
            view.doClick();
        });

        panel.setLayout(new BorderLayout());
        panel.add(form,BorderLayout.WEST);
        panel.add(sp,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTenantsPanel() {
        JPanel panel = makePanelWithBorder("Manage Tenants");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(panelColor);
        GridBagConstraints c = new GridBagConstraints();
        c.insets=new Insets(6,6,6,6);

        JLabel nL = new JLabel("Tenant Name:"); c.gridx=0;c.gridy=0;form.add(nL,c);
        JTextField nF=new JTextField(15); c.gridx=1;form.add(nF,c);
        JLabel pL=new JLabel("Phone:"); c.gridx=0;c.gridy=1;form.add(pL,c);
        JTextField pF=new JTextField(15); c.gridx=1;form.add(pF,c);
        JButton add=new JButton("Add Tenant"); JButton view=new JButton("View Tenants");
        JButton update=new JButton("Update Selected"); JButton delete=new JButton("Delete Selected");
        JPanel btns=new JPanel(); btns.add(add); btns.add(view); btns.add(update); btns.add(delete);
        c.gridx=0;c.gridy=2;c.gridwidth=2;form.add(btns,c);

        DefaultTableModel model=new DefaultTableModel(new String[]{"ID","Name","Phone"},0);
        JTable table=new JTable(model);
        JScrollPane sp=new JScrollPane(table);

        table.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                int r = table.getSelectedRow();
                if(r>=0){
                    nF.setText(model.getValueAt(r,1)+"");
                    pF.setText(model.getValueAt(r,2)+"");
                }
            }
        });

        add.addActionListener(e->{
            try{
                String name = nF.getText().trim();
                String phone = pF.getText().trim();
                if(!isValidString(name)) throw new Exception("Tenant name required");
                if(!isValidPhone(phone)) throw new Exception("Invalid phone number");
                db.insertTenant(name,phone);
                JOptionPane.showMessageDialog(this,"Tenant added successfully!");
                clearFields(nF,pF);
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });
        view.addActionListener(e->{
            if("tenant".equals(currentUserRole)){
                if(currentUserRefId!=null) refreshTable(model, db.getTenantById(currentUserRefId));
                else refreshTable(model, db.getAll("tenants"));
            } else {
                refreshTable(model,db.getAll("tenants"));
            }
        });

        update.addActionListener(e->{
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select tenant to update"); return; }
            try{
                int id = Integer.parseInt(model.getValueAt(r,0).toString());
                String name = nF.getText().trim();
                String phone = pF.getText().trim();
                if(!isValidString(name)) throw new Exception("Tenant name required");
                if(!isValidPhone(phone)) throw new Exception("Invalid phone");
                db.updateTenant(id,name,phone);
                JOptionPane.showMessageDialog(this,"Updated");
                view.doClick();
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        delete.addActionListener(e->{
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select tenant to delete"); return; }
            int id = Integer.parseInt(model.getValueAt(r,0).toString());
            int conf = JOptionPane.showConfirmDialog(this,"Delete tenant?","Confirm",JOptionPane.YES_NO_OPTION);
            if(conf!=JOptionPane.YES_OPTION) return;
            db.deleteById("tenants", id);
            JOptionPane.showMessageDialog(this,"Deleted");
            view.doClick();
        });

        panel.setLayout(new BorderLayout());
        panel.add(form,BorderLayout.WEST);
        panel.add(sp,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLeasesPanel() {
        JPanel panel = makePanelWithBorder("Manage Leases");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(panelColor);
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(6,6,6,6);

        JLabel prop=new JLabel("Property ID:"); c.gridx=0;c.gridy=0;form.add(prop,c);
        JTextField propF=new JTextField(10); c.gridx=1;form.add(propF,c);
        JLabel ten=new JLabel("Tenant ID:"); c.gridx=0;c.gridy=1;form.add(ten,c);
        JTextField tenF=new JTextField(10); c.gridx=1;form.add(tenF,c);
        JLabel start=new JLabel("Start Date (yyyy-MM-dd):"); c.gridx=0;c.gridy=2;form.add(start,c);
        JTextField startF=new JTextField(10); c.gridx=1;form.add(startF,c);
        JLabel end=new JLabel("End Date (yyyy-MM-dd):"); c.gridx=0;c.gridy=3;form.add(end,c);
        JTextField endF=new JTextField(10); c.gridx=1;form.add(endF,c);

        JButton add=new JButton("Add Lease"); JButton view=new JButton("View Leases");
        JButton update=new JButton("Update Selected"); JButton delete=new JButton("Delete Selected");
        JPanel btns=new JPanel(); btns.add(add); btns.add(view); btns.add(update); btns.add(delete);
        c.gridx=0;c.gridy=4;c.gridwidth=2;form.add(btns,c);

        DefaultTableModel model=new DefaultTableModel(new String[]{"ID","PropertyID","TenantID","Start","End"},0);
        JTable table=new JTable(model);
        JScrollPane sp=new JScrollPane(table);

        table.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                int r = table.getSelectedRow();
                if(r>=0){
                    propF.setText(model.getValueAt(r,1)+"");
                    tenF.setText(model.getValueAt(r,2)+"");
                    startF.setText(model.getValueAt(r,3)+"");
                    endF.setText(model.getValueAt(r,4)+"");
                }
            }
        });

        add.addActionListener(e->{
            try{
                String propId = propF.getText().trim();
                String tenId = tenF.getText().trim();
                String sdate = startF.getText().trim();
                String edate = endF.getText().trim();
                if(!isNumeric(propId)||!isNumeric(tenId)) throw new Exception("Property ID and Tenant ID must be numeric");
                validateDateString(sdate);
                validateDateString(edate);
                db.insertLease(propId,tenId,sdate,edate);
                JOptionPane.showMessageDialog(this,"Lease added successfully!");
                clearFields(propF,tenF,startF,endF);
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });
        view.addActionListener(e->{
            if("tenant".equals(currentUserRole)){
                // show only leases for current tenant
                if(currentUserRefId!=null) refreshTable(model, db.getLeasesForTenant(currentUserRefId));
                else refreshTable(model, db.getAll("leases"));
            } else {
                refreshTable(model,db.getAll("leases"));
            }
        });

        update.addActionListener(e->{
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select lease to update"); return; }
            try{
                int id = Integer.parseInt(model.getValueAt(r,0).toString());
                String propId = propF.getText().trim();
                String tenId = tenF.getText().trim();
                String sdate = startF.getText().trim();
                String edate = endF.getText().trim();
                if(!isNumeric(propId)||!isNumeric(tenId)) throw new Exception("Property ID and Tenant ID must be numeric");
                validateDateString(sdate); validateDateString(edate);
                db.updateLease(id, propId, tenId, sdate, edate);
                JOptionPane.showMessageDialog(this,"Updated");
                view.doClick();
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        delete.addActionListener(e->{
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select lease to delete"); return; }
            int id = Integer.parseInt(model.getValueAt(r,0).toString());
            int conf = JOptionPane.showConfirmDialog(this,"Delete lease?","Confirm",JOptionPane.YES_NO_OPTION);
            if(conf!=JOptionPane.YES_OPTION) return;
            db.deleteById("leases", id);
            JOptionPane.showMessageDialog(this,"Deleted");
            view.doClick();
        });

        panel.setLayout(new BorderLayout());
        panel.add(form,BorderLayout.WEST);
        panel.add(sp,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPaymentsPanel() {
        JPanel panel = makePanelWithBorder("Manage Payments");
        JPanel form=new JPanel(new GridBagLayout());
        form.setBackground(panelColor);
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(6,6,6,6);

        JLabel lL=new JLabel("Lease ID:"); c.gridx=0;c.gridy=0;form.add(lL,c);
        JTextField lF=new JTextField(10); c.gridx=1;form.add(lF,c);
        JLabel amt=new JLabel("Amount:"); c.gridx=0;c.gridy=1;form.add(amt,c);
        JTextField aF=new JTextField(10); c.gridx=1;form.add(aF,c);
        JLabel date=new JLabel("Date (yyyy-MM-dd):"); c.gridx=0;c.gridy=2;form.add(date,c);
        JTextField dF=new JTextField(10); c.gridx=1;form.add(dF,c);

        JButton add=new JButton("Add Payment"); JButton view=new JButton("View Payments");
        JButton update=new JButton("Update Selected"); JButton delete=new JButton("Delete Selected");
        JPanel btns=new JPanel(); btns.add(add); btns.add(view); btns.add(update); btns.add(delete);
        c.gridx=0;c.gridy=3;c.gridwidth=2;form.add(btns,c);

        DefaultTableModel model=new DefaultTableModel(new String[]{"ID","LeaseID","Amount","Date"},0);
        JTable table=new JTable(model);
        JScrollPane sp=new JScrollPane(table);

        table.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                int r = table.getSelectedRow();
                if(r>=0){
                    lF.setText(model.getValueAt(r,1)+"");
                    aF.setText(model.getValueAt(r,2)+"");
                    dF.setText(model.getValueAt(r,3)+"");
                }
            }
        });

        add.addActionListener(e->{
            try{
                String leaseId = lF.getText().trim();
                String amount = aF.getText().trim();
                String dateS = dF.getText().trim();
                if(!isNumeric(leaseId)) throw new Exception("Lease ID must be numeric");
                double amtVal = parsePositiveDouble(amount, "Amount must be positive");
                validateDateString(dateS);
                db.insertPayment(leaseId, String.valueOf(amtVal), dateS);
                JOptionPane.showMessageDialog(this,"Payment added successfully!");
                clearFields(lF,aF,dF);
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        view.addActionListener(e->{
            if("tenant".equals(currentUserRole)){
                // show payments for this tenant by joining leases <-> payments
                if(currentUserRefId!=null) refreshTable(model, db.getPaymentsForTenant(currentUserRefId));
                else refreshTable(model, db.getAll("payments"));
            } else {
                refreshTable(model, db.getAll("payments"));
            }
        });

        update.addActionListener(e->{
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select payment to update"); return; }
            try{
                int id = Integer.parseInt(model.getValueAt(r,0).toString());
                String leaseId = lF.getText().trim();
                String amount = aF.getText().trim();
                String dateS = dF.getText().trim();
                if(!isNumeric(leaseId)) throw new Exception("Lease ID must be numeric");
                double amtVal = parsePositiveDouble(amount, "Amount must be positive");
                validateDateString(dateS);
                db.updatePayment(id, leaseId, amtVal, dateS);
                JOptionPane.showMessageDialog(this,"Updated");
                view.doClick();
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        delete.addActionListener(e->{
            int r = table.getSelectedRow();
            if(r<0){ JOptionPane.showMessageDialog(this,"Select payment to delete"); return; }
            int id = Integer.parseInt(model.getValueAt(r,0).toString());
            int conf = JOptionPane.showConfirmDialog(this,"Delete payment?","Confirm",JOptionPane.YES_NO_OPTION);
            if(conf!=JOptionPane.YES_OPTION) return;
            db.deleteById("payments", id);
            JOptionPane.showMessageDialog(this,"Deleted");
            view.doClick();
        });

        panel.setLayout(new BorderLayout());
        panel.add(form,BorderLayout.WEST);
        panel.add(sp,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = makePanelWithBorder("Search Properties");
        panel.setLayout(new BorderLayout());
        JPanel top=new JPanel(new FlowLayout());
        JTextField qF=new JTextField(20);
        JButton search=new JButton("Search");
        JButton all=new JButton("Show All");
        top.add(new JLabel("Property Name:"));
        top.add(qF); top.add(search); top.add(all);

        // additional filters
        top.add(new JLabel("Location:"));
        JTextField locF = new JTextField(10); top.add(locF);
        top.add(new JLabel("Min Rent:"));
        JTextField minF = new JTextField(6); top.add(minF);
        top.add(new JLabel("Max Rent:"));
        JTextField maxF = new JTextField(6); top.add(maxF);
        top.add(new JLabel("Owner ID:"));
        JTextField ownerF = new JTextField(6); top.add(ownerF);
        JButton filterBtn = new JButton("Apply Filters");
        top.add(filterBtn);

        panel.add(top,BorderLayout.NORTH);

        DefaultTableModel model=new DefaultTableModel(new String[]{"ID","Name","Location","Rent","OwnerID"},0);
        JTable table=new JTable(model);
        JScrollPane sp=new JScrollPane(table);
        panel.add(sp,BorderLayout.CENTER);

        search.addActionListener(e->refreshTable(model,db.searchProperty(qF.getText())));
        all.addActionListener(e->refreshTable(model,db.getAll("properties")));

        filterBtn.addActionListener(e->{
            String loc = locF.getText().trim();
            String min = minF.getText().trim();
            String max = maxF.getText().trim();
            String ownerId = ownerF.getText().trim();
            try {
                refreshTable(model, db.searchPropertiesWithFilters(loc,min,max,ownerId));
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        return panel;
    }

    private JPanel createReportsPanel(){
        JPanel panel = makePanelWithBorder("Payment Reports & Alerts");
        panel.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Report Month (yyyy-MM):"));
        JTextField monthF = new JTextField(8); top.add(monthF);
        JButton gen = new JButton("Generate Monthly Report");
        JButton unpaid = new JButton("Show Unpaid Rent Alerts");
        top.add(gen); top.add(unpaid);

        panel.add(top, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{"LeaseID","PropertyID","TenantID","Amount","Date","Notes"},0);
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        gen.addActionListener(e->{
            String m = monthF.getText().trim();
            if(!Pattern.matches("\\d{4}-\\d{2}", m)){ JOptionPane.showMessageDialog(this,"Enter month as yyyy-MM"); return; }
            try{
                refreshTable(model, db.getMonthlyPaymentsReport(m));
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        unpaid.addActionListener(e->{
            String m = monthF.getText().trim();
            if(!Pattern.matches("\\d{4}-\\d{2}", m)){ JOptionPane.showMessageDialog(this,"Enter month as yyyy-MM"); return; }
            try{
                refreshTable(model, db.getUnpaidLeasesForMonth(m));
            }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
        });

        return panel;
    }

    // ---------------- UTILITIES ----------------
    private JPanel makePanelWithBorder(String title){
        JPanel p=new JPanel();
        p.setBackground(bgColor);
        p.setBorder(BorderFactory.createTitledBorder(null,title,TitledBorder.LEFT,TitledBorder.TOP,labelFont));
        return p;
    }

    private void refreshTable(DefaultTableModel model, ResultSet rs){
        try{
            model.setRowCount(0);
            if(rs==null) return;
            ResultSetMetaData md=rs.getMetaData();
            int cols=md.getColumnCount();
            while(rs.next()){
                Vector<Object> row=new Vector<>();
                for(int i=1;i<=cols;i++) row.add(rs.getObject(i));
                model.addRow(row);
            }
        }catch(Exception e){ e.printStackTrace(); }
    }

    private void clearFields(JTextField... f){for(JTextField x:f)x.setText("");}

    // ---------------- Validation Helpers ----------------
    private boolean isValidPhone(String p){
        if(p==null) return false;
        // simple 7-15 digits, optional + at start and spaces/dashes
        return Pattern.matches("^\\+?\\d[\\d\\- ]{6,14}\\d$", p);
    }
    private boolean isValidString(String s){ return s!=null && !s.trim().isEmpty(); }
    private boolean isValidLocation(String s){ return isValidString(s); }
    private boolean isNumeric(String s){ if(s==null) return false; try{ Integer.parseInt(s); return true;}catch(Exception e){return false;} }
    private double parsePositiveDouble(String s, String err) throws Exception {
        try{ double v = Double.parseDouble(s); if(v<=0) throw new Exception(err); return v; }catch(NumberFormatException ex){ throw new Exception(err); }
    }
    private void validateDateString(String s) throws Exception {
        if(s==null) throw new Exception("Date required");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(false);
        try{ df.parse(s); }catch(ParseException ex){ throw new Exception("Invalid date, use yyyy-MM-dd"); }
    }

    // ---------------- ROLE ACCESS ----------------
    private void applyRoleAccess(){
        // owners: hide Owners list? they can only view their properties
        // tenants: disable Owners and Properties tabs
        if("owner".equals(currentUserRole)){
            // disable tenant management for owners
            setTabEnabledByName("Owners", true); // owners can view their own owner info
            setTabEnabledByName("Tenants", false);
            // filter on properties tab will be applied on view
        } else if("tenant".equals(currentUserRole)){
            setTabEnabledByName("Owners", false);
            setTabEnabledByName("Properties", false);
            setTabEnabledByName("Reports", false); // maybe not needed
        } else {
            // admin - full access
        }
    }
    private void setTabEnabledByName(String title, boolean enabled){
        for(int i=0;i<tabs.getTabCount();i++){
            if(tabs.getTitleAt(i).equals(title)){
                tabs.setEnabledAt(i, enabled);
                break;
            }
        }
    }

    // ---------------- DB HELPER ----------------
    private static class DBHelper {
        private Connection conn;
        DBHelper(String url,String user,String pass){
            try{
                conn=DriverManager.getConnection(url,user,pass);
                System.out.println("✅ Connected to MySQL!");
            }catch(Exception e){
                JOptionPane.showMessageDialog(null,"DB connection failed: "+e.getMessage());
                e.printStackTrace();
            }
        }

        void createTablesIfNotExist(){
            try(Statement st=conn.createStatement()){
                // core tables
                st.executeUpdate("CREATE TABLE IF NOT EXISTS owners (id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100),phone VARCHAR(50))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS properties (id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100),location VARCHAR(100),rent DOUBLE,owner_id INT,FOREIGN KEY(owner_id) REFERENCES owners(id) ON DELETE SET NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS tenants (id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100),phone VARCHAR(50))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS leases (id INT AUTO_INCREMENT PRIMARY KEY,property_id INT,tenant_id INT,start_date DATE,end_date DATE,FOREIGN KEY(property_id) REFERENCES properties(id) ON DELETE CASCADE,FOREIGN KEY(tenant_id) REFERENCES tenants(id) ON DELETE CASCADE)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS payments (id INT AUTO_INCREMENT PRIMARY KEY,lease_id INT,amount DOUBLE,date DATE,FOREIGN KEY(lease_id) REFERENCES leases(id) ON DELETE CASCADE)");
                // users - for login/signup
                st.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) UNIQUE, password_hash VARCHAR(256), role VARCHAR(20), ref_id INT)");
            }catch(Exception e){e.printStackTrace();}
        }

        // ---------- auth ----------
        boolean createUser(String username, String password, String role, Integer refId) throws Exception {
            // check exist
            try(PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")){
                ps.setString(1,username);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) return false;
            }
            String hash = sha256(password);
            try(PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username,password_hash,role,ref_id) VALUES(?,?,?,?)")){
                ps.setString(1,username);
                ps.setString(2,hash);
                ps.setString(3,role);
                if(refId==null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, refId);
                ps.executeUpdate();
                return true;
            }
        }
        Integer authenticate(String username, String password) throws Exception {
            String hash = sha256(password);
            try(PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password_hash = ?")){
                ps.setString(1,username); ps.setString(2,hash);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) return rs.getInt(1);
                else return null;
            }
        }
        UserInfo getUserInfoById(int id) throws Exception {
            try(PreparedStatement ps = conn.prepareStatement("SELECT role, ref_id FROM users WHERE id = ?")){
                ps.setInt(1,id);
                ResultSet rs = ps.executeQuery();
                if(rs.next()){
                    String role = rs.getString("role");
                    int refId = rs.getInt("ref_id");
                    if(rs.wasNull()) return new UserInfo(role, null);
                    return new UserInfo(role, refId);
                }
                return null;
            }
        }
        private String sha256(String s) throws Exception {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for(byte b: d) sb.append(String.format("%02x", b));
            return sb.toString();
        }

        // ---------- inserts ----------
        void insertOwner(String n,String p){ runUpdate("INSERT INTO owners(name,phone) VALUES(?,?)", n, p); }
        void insertTenant(String n,String p){ runUpdate("INSERT INTO tenants(name,phone) VALUES(?,?)", n, p); }
        void insertProperty(String n,String l,Double r,String o){
            runUpdate("INSERT INTO properties(name,location,rent,owner_id) VALUES(?,?,?,?)", n, l, r, (o==null?null:Integer.parseInt(o)));
        }
        void insertLease(String p,String t,String s,String e){
            runUpdate("INSERT INTO leases(property_id,tenant_id,start_date,end_date) VALUES(?,?,?,?)", Integer.parseInt(p), Integer.parseInt(t), Date.valueOf(s), Date.valueOf(e));
        }
        void insertPayment(String l,String a,String d){
            runUpdate("INSERT INTO payments(lease_id,amount,date) VALUES(?,?,?)", Integer.parseInt(l), Double.parseDouble(a), Date.valueOf(d));
        }

        // ---------- updates ----------
        void updateOwner(int id, String name, String phone){ runUpdate("UPDATE owners SET name=?, phone=? WHERE id=?", name, phone, id); }
        void updateTenant(int id, String name, String phone){ runUpdate("UPDATE tenants SET name=?, phone=? WHERE id=?", name, phone, id); }
        void updateProperty(int id, String name, String location, double rent, String ownerId){
            runUpdate("UPDATE properties SET name=?, location=?, rent=?, owner_id=? WHERE id=?", name, location, rent, (ownerId==null?null:Integer.parseInt(ownerId)), id);
        }
        void updateLease(int id, String propertyId, String tenantId, String start, String end){
            runUpdate("UPDATE leases SET property_id=?, tenant_id=?, start_date=?, end_date=? WHERE id=?", Integer.parseInt(propertyId), Integer.parseInt(tenantId), Date.valueOf(start), Date.valueOf(end), id);
        }
        void updatePayment(int id, String leaseId, double amount, String date){ runUpdate("UPDATE payments SET lease_id=?, amount=?, date=? WHERE id=?", Integer.parseInt(leaseId), amount, Date.valueOf(date), id); }

        // ---------- delete ----------
        void deleteById(String table, int id){
            runUpdate("DELETE FROM " + table + " WHERE id = ?", id);
        }

        // ---------- generic ----------
        void runUpdate(String sql,Object...params){
            try(PreparedStatement ps = conn.prepareStatement(sql)){
                for(int i=0;i<params.length;i++){
                    Object p = params[i];
                    if(p==null) ps.setNull(i+1, Types.NULL);
                    else if(p instanceof Integer) ps.setInt(i+1, (Integer)p);
                    else if(p instanceof Double) ps.setDouble(i+1, (Double)p);
                    else if(p instanceof java.sql.Date) ps.setDate(i+1, (java.sql.Date)p);
                    else ps.setObject(i+1, p);
                }
                ps.executeUpdate();
            }catch(Exception e){ e.printStackTrace(); }
        }

        ResultSet getAll(String table){
            try{
                Statement st=conn.createStatement();
                return st.executeQuery("SELECT * FROM "+table);
            }catch(Exception e){e.printStackTrace();return null;}
        }

        // ---------- convenience queries ----------
        ResultSet searchProperty(String q){
            try{
                PreparedStatement ps=conn.prepareStatement("SELECT * FROM properties WHERE name LIKE ?");
                ps.setString(1,"%"+q+"%");
                return ps.executeQuery();
            }catch(Exception e){e.printStackTrace();return null;}
        }

        ResultSet getOwnerById(int id){
            try{
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM owners WHERE id = ?");
                ps.setInt(1,id);
                return ps.executeQuery();
            }catch(Exception e){ e.printStackTrace(); return null; }
        }

        ResultSet getTenantById(int id){
            try{
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM tenants WHERE id = ?");
                ps.setInt(1,id);
                return ps.executeQuery();
            }catch(Exception e){ e.printStackTrace(); return null; }
        }

        ResultSet getPropertiesForOwner(Integer ownerId){
            try{
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM properties WHERE owner_id = ?");
                if(ownerId==null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, ownerId);
                return ps.executeQuery();
            }catch(Exception e){ e.printStackTrace(); return null; }
        }

        ResultSet getLeasesForTenant(Integer tenantId){
            try{
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM leases WHERE tenant_id = ?");
                if(tenantId==null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, tenantId);
                return ps.executeQuery();
            }catch(Exception e){ e.printStackTrace(); return null; }
        }

        ResultSet getPaymentsForTenant(Integer tenantId){
            try{
                // join payments -> leases -> tenants
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.id, p.lease_id, p.amount, p.date FROM payments p " +
                    "JOIN leases l ON p.lease_id = l.id WHERE l.tenant_id = ?");
                if(tenantId==null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, tenantId);
                return ps.executeQuery();
            }catch(Exception e){ e.printStackTrace(); return null; }
        }

        // search with filters
        ResultSet searchPropertiesWithFilters(String location, String minRent, String maxRent, String ownerId) throws Exception {
            StringBuilder sql = new StringBuilder("SELECT * FROM properties WHERE 1=1 ");
            java.util.List<Object> params = new java.util.ArrayList<>();
            if(location!=null && !location.isEmpty()){ sql.append(" AND location LIKE ?"); params.add("%"+location+"%"); }
            if(minRent!=null && !minRent.isEmpty()){ double v = Double.parseDouble(minRent); sql.append(" AND rent >= ?"); params.add(v); }
            if(maxRent!=null && !maxRent.isEmpty()){ double v = Double.parseDouble(maxRent); sql.append(" AND rent <= ?"); params.add(v); }
            if(ownerId!=null && !ownerId.isEmpty()){ int v = Integer.parseInt(ownerId); sql.append(" AND owner_id = ?"); params.add(v); }
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for(int i=0;i<params.size();i++) ps.setObject(i+1, params.get(i));
            return ps.executeQuery();
        }

        // ---------- reports ----------
        ResultSet getMonthlyPaymentsReport(String yyyyMM) throws Exception {
            // payments in the month
            String start = yyyyMM + "-01";
            // compute end as next month first day
            java.time.YearMonth ym = java.time.YearMonth.parse(yyyyMM);
            java.time.LocalDate startD = ym.atDay(1);
            java.time.LocalDate endD = ym.atEndOfMonth();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT p.id as payment_id, p.lease_id, l.property_id, l.tenant_id, p.amount, p.date FROM payments p " +
                "JOIN leases l ON p.lease_id = l.id WHERE p.date BETWEEN ? AND ? ORDER BY p.date");
            ps.setDate(1, java.sql.Date.valueOf(startD));
            ps.setDate(2, java.sql.Date.valueOf(endD));
            return ps.executeQuery();
        }

        ResultSet getUnpaidLeasesForMonth(String yyyyMM) throws Exception {
            // Return leases with no payments in that month (simple approach)
            java.time.YearMonth ym = java.time.YearMonth.parse(yyyyMM);
            java.time.LocalDate startD = ym.atDay(1);
            java.time.LocalDate endD = ym.atEndOfMonth();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT l.id AS lease_id, l.property_id, l.tenant_id, l.start_date, l.end_date, " +
                "NULL AS amount, NULL AS date, 'No payment in month' AS notes " +
                "FROM leases l WHERE NOT EXISTS (SELECT 1 FROM payments p WHERE p.lease_id = l.id AND p.date BETWEEN ? AND ?)");
            ps.setDate(1, java.sql.Date.valueOf(startD));
            ps.setDate(2, java.sql.Date.valueOf(endD));
            return ps.executeQuery();
        }

        void close(){try{if(conn!=null)conn.close();}catch(Exception ignored){}}
    }

    private static class UserInfo {
        String role;
        Integer refId;
        UserInfo(String r, Integer ref){ role=r; refId=ref; }
    }

    public static void main(String[] args){
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
        SwingUtilities.invokeLater(RentalApp::new);
    }
}
