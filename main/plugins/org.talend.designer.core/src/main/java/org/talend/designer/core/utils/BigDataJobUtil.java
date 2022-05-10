// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.talend.commons.exception.PersistenceException;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.hadoop.HadoopConstants;
import org.talend.core.hadoop.version.EHadoopDistributions;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.JobletProcessItem;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.repository.ui.editor.RepositoryEditorInput;
import org.talend.core.ui.IJobletProviderService;
import org.talend.core.ui.ISparkJobletProviderService;
import org.talend.core.ui.ISparkStreamingJobletProviderService;
import org.talend.designer.core.ICreateMRProcessService;
import org.talend.designer.core.ICreateStormProcessService;
import org.talend.designer.core.ui.MultiPageTalendEditor;
import org.talend.designer.core.ui.editor.ProcessEditorInput;

/**
 * created by nrousseau on Mar 24, 2018 Detailled comment
 *
 */
public class BigDataJobUtil {

    private IProcess process;

    public BigDataJobUtil(IProcess process) {
        this.process = process;
    }

    /**
     * DOC nrousseau Comment method "needsShade".
     *
     * @param item
     */
    public boolean needsShade() {
        if (isMRWithHDInsight()) {
            return true;
        }
        if (isBDJobWithFramework(ERepositoryObjectType.PROCESS_STORM, HadoopConstants.FRAMEWORK_STORM)) {
            return true;
        }
        return isSparkWithHDInsight();
    }

    /**
     * DOC nrousseau Comment method "isSparkWithHDInsight".
     *
     * @param isSparkWithHDInsight
     * @return
     */
    public boolean isSparkWithHDInsight() {
        boolean isSparkWithHDInsight = false;
        if (isBDJobWithFramework(ERepositoryObjectType.PROCESS_MR, HadoopConstants.FRAMEWORK_SPARK)
                || isBDJobWithFramework(ERepositoryObjectType.PROCESS_STORM, HadoopConstants.FRAMEWORK_SPARKSTREAMING)) {
            List<? extends IElementParameter> parameters = process.getElementParametersWithChildrens();
            for (IElementParameter pt : parameters) {
                if (pt.getName().equals("DISTRIBUTION") //$NON-NLS-1$
                        && EHadoopDistributions.MICROSOFT_HD_INSIGHT.getName().equals(pt.getValue())) {
                    isSparkWithHDInsight = true;
                }
            }
        }
        return isSparkWithHDInsight;
    }
    
    public boolean isSparkWithSynapse() {
        boolean isSparkWithSynapse = false;
        if (isBDJobWithFramework(ERepositoryObjectType.PROCESS_MR, HadoopConstants.FRAMEWORK_SPARK)
                || isBDJobWithFramework(ERepositoryObjectType.PROCESS_STORM, HadoopConstants.FRAMEWORK_SPARKSTREAMING)) {
            List<? extends IElementParameter> parameters = process.getElementParametersWithChildrens();
            for (IElementParameter pt : parameters) {
                if (pt.getName().equals("DISTRIBUTION") //$NON-NLS-1$
                        && EHadoopDistributions.AZURE_SYNAPSE.getName().equals(pt.getValue())) {
                    isSparkWithSynapse = true;
                }
            }
        }
        return isSparkWithSynapse;
    }

    public boolean isMRWithHDInsight() {
        Boolean isMRWithHDInsight = false;
        if (process != null) {
            isMRWithHDInsight = false;
            if (isBDJobWithFramework(ERepositoryObjectType.PROCESS_MR, HadoopConstants.FRAMEWORK_MAPREDUCE)) {
                List<? extends IElementParameter> parameters = process.getElementParametersWithChildrens();
                for (IElementParameter pt : parameters) {
                    if (pt.getName().equals("DISTRIBUTION") //$NON-NLS-1$
                            && EHadoopDistributions.MICROSOFT_HD_INSIGHT.getName().equals(pt.getValue())) {
                        isMRWithHDInsight = true;
                        break;
                    }
                }
            }
        }
        return isMRWithHDInsight;
    }

    /**
     * Test if it is a spark job with yarn cluster mode
     */
    private boolean isSparkWithYarnClusterMode() {
        Boolean isSparkInYarnClusterMode = false;
        // Test if we are in Spark or Spark streaming
        if (isBDJobWithFramework(ERepositoryObjectType.PROCESS_MR, HadoopConstants.FRAMEWORK_SPARK)
                || isBDJobWithFramework(ERepositoryObjectType.PROCESS_STORM, HadoopConstants.FRAMEWORK_SPARKSTREAMING)) {

            List<? extends IElementParameter> parameters = process.getElementParametersWithChildrens();
            for (IElementParameter pt : parameters) {
                if (HadoopConstants.SPARK_MODE.equals(pt.getName())
                        && HadoopConstants.SPARK_MODE_YARN_CLUSTER.equals(pt.getValue())) {
                    isSparkInYarnClusterMode = true;
                    break;
                }
            }
        }
        return isSparkInYarnClusterMode;
    }

    private boolean isBDJobWithFramework(ERepositoryObjectType objectType, String frameworkName) {
        // Storm/SparkStreaming(PROCESS_STORM), MR/Spark(PROCESS_MR)
        if (process != null && process instanceof IProcess2 && ((IProcess2) process).getAdditionalProperties() != null
                && frameworkName.equals(((IProcess2) process).getAdditionalProperties().get(HadoopConstants.FRAMEWORK))) {
            return true;
        }
        return false;
    }

    /** Find the distribution where the generated jar rquired to have the context files inside **/
    public boolean needsToHaveContextInsideJar() {
        List<? extends IElementParameter> parameters = process.getElementParametersWithChildrens();

        if (process != null && parameters != null) {

            for (IElementParameter pt : parameters) {
                if (pt.getName().equals("DISTRIBUTION")) { //$NON-NLS-1$
                    String value = String.valueOf(pt.getValue());
                    if ("MICROSOFT_HD_INSIGHT".equals(value) //$NON-NLS-1$
                            || "GOOGLE_CLOUD_DATAPROC".equals(value) //$NON-NLS-1$
                            || "DATABRICKS".equals(value)) { //$NON-NLS-1$
                        return true;
                    }
                }
            }
            if (isSparkWithYarnClusterMode()) {
                return true;
            }
        }
        return false;
    }

    public void setExcludedModules(Collection<ModuleNeeded> modulesNeeded) {
        if (isMRWithHDInsight() || isSparkWithHDInsight()) {
            // we need to exclude every non-MR Required jars.
            for (ModuleNeeded currentModule : modulesNeeded) {
                if (currentModule.isMrRequired()) {
                    currentModule.setExcluded(true);
                }
            }
        }
    }

    public void removeExcludedModules(Collection<ModuleNeeded> modulesNeeded) {
        Iterator<ModuleNeeded> itModules = modulesNeeded.iterator();
        while (itModules.hasNext()) {
            ModuleNeeded module = itModules.next();
            if (module.isExcluded()) {
                itModules.remove();
            }
        }
    }

    /**
     * DOC nrousseau Comment method "getShadedModulesExclude".
     *
     * @param modulesNeeded
     * @return
     */
    public Set<ModuleNeeded> getShadedModulesExclude(Set<ModuleNeeded> modulesNeeded) {
        Set<ModuleNeeded> excludedModules = new HashSet<>();
        if (isMRWithHDInsight() || isSparkWithHDInsight()) {
            // we need to exclude every non-MR Required jars.
            for (ModuleNeeded currentModule : modulesNeeded) {
                if (!currentModule.isMrRequired()) {
                    excludedModules.add(currentModule);
                } else {
                    currentModule.setExcluded(true);
                }
            }
        }

        return excludedModules;
    }

    public static RepositoryEditorInput getProcessItemEditorInput(final Item item, final boolean readonly)
            throws PersistenceException {
        RepositoryEditorInput fileEditorInput = null;
        ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(item);
        if (item instanceof ProcessItem) {
            ProcessItem processItem = (ProcessItem) item;
            if (ERepositoryObjectType.PROCESS_STORM == itemType) {
                if (GlobalServiceRegister.getDefault().isServiceRegistered(ICreateStormProcessService.class)) {
                    ICreateStormProcessService stromService = GlobalServiceRegister.getDefault()
                            .getService(ICreateStormProcessService.class);
                    fileEditorInput = stromService.getStromProcessEditorInput(processItem, true, false, readonly);
                }
            } else if (ERepositoryObjectType.PROCESS_MR == itemType) {
                if (GlobalServiceRegister.getDefault().isServiceRegistered(ICreateMRProcessService.class)) {
                    ICreateMRProcessService mRService = GlobalServiceRegister.getDefault()
                            .getService(ICreateMRProcessService.class);
                    fileEditorInput = mRService.getMRProcessEditorInput(processItem, true, false, readonly);
                }
            } else {
                fileEditorInput = new ProcessEditorInput(processItem, true, false, readonly);
            }
        } else if (item instanceof JobletProcessItem) {
            JobletProcessItem jobletItem = (JobletProcessItem) item;
            if (ERepositoryObjectType.SPARK_JOBLET == itemType) {
                if (GlobalServiceRegister.getDefault().isServiceRegistered(ISparkJobletProviderService.class)) {
                    ISparkJobletProviderService sparkJobletService = GlobalServiceRegister.getDefault()
                            .getService(ISparkJobletProviderService.class);
                    if (sparkJobletService != null && sparkJobletService.isSparkJobletItem(jobletItem)) {
                        fileEditorInput = (RepositoryEditorInput) sparkJobletService.createJobletEditor(jobletItem, true, false,
                                readonly, false);
                    }
                }
            } else if (ERepositoryObjectType.SPARK_STREAMING_JOBLET == itemType) {
                if (GlobalServiceRegister.getDefault().isServiceRegistered(ISparkStreamingJobletProviderService.class)) {
                    ISparkStreamingJobletProviderService sparkJobletService = GlobalServiceRegister.getDefault()
                            .getService(ISparkStreamingJobletProviderService.class);
                    if (sparkJobletService != null && sparkJobletService.isSparkStreamingJobletItem(jobletItem)) {
                        fileEditorInput = (RepositoryEditorInput) sparkJobletService.createJobletEditor(jobletItem, true, false,
                                readonly, false);
                    }
                }
            } else {
                if (GlobalServiceRegister.getDefault().isServiceRegistered(IJobletProviderService.class)) {
                    IJobletProviderService jobletService = GlobalServiceRegister.getDefault()
                            .getService(IJobletProviderService.class);
                    if (jobletService != null) {
                        fileEditorInput = (RepositoryEditorInput) jobletService.getJobletProcessEditorInput(jobletItem, true,
                                false, readonly, false);
                    }
                }
            }
        }
        return fileEditorInput;
    }

    public static String getEditorId(ERepositoryObjectType itemType) throws PersistenceException {
        String editorId = null;
        if (ERepositoryObjectType.PROCESS_STORM == itemType) {
            editorId = "org.talend.repository.storm.editor";//$NON-NLS-1$
        } else if (ERepositoryObjectType.PROCESS_MR == itemType) {
            editorId = "org.talend.designer.mapreduce.editor"; //$NON-NLS-1$
        } else if (ERepositoryObjectType.SPARK_JOBLET == itemType) {
            editorId = "org.talend.designer.sparkjoblet.ui.editors.SparkJobletMultiPageTalendEditor"; //$NON-NLS-1$
        } else if (ERepositoryObjectType.SPARK_STREAMING_JOBLET == itemType) {
            editorId = "org.talend.designer.sparkstreamingjoblet.ui.editors.SparkStreamingJobletMultiPageTalendEditor"; //$NON-NLS-1$
        } else if (ERepositoryObjectType.JOBLET == itemType) {
            editorId = "org.talend.designer.joblet.multieditor"; //$NON-NLS-1$
        } else {
            editorId = MultiPageTalendEditor.ID;
        }
        return editorId;
    }

}
