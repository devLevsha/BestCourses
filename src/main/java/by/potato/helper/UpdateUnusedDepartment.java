package by.potato.helper;

import by.potato.db.DataBaseHelper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class UpdateUnusedDepartment implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        DataBaseHelper.getInstance().deleteUnusedDepartment();
    }
}
