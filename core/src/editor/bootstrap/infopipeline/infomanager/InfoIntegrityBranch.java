package editor.bootstrap.infopipeline.infomanager;

import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoissue.InfoIssueStruct;
import editor.bootstrap.infopipeline.infolink.InfoLinkStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.util.InfoReferenceKind;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class InfoIntegrityBranch extends BranchPackage {

    /*
     * Keeps what lies on disk loadable by the game. A set of files may only
     * be written when none of them holds an error — a key the game ignores is
     * no error — every entry they name will
     * be on disk once they are written — saved already, or written with them
     * — and no name they take away is still named by a file that stays on
     * disk as it is. A file or entry may only be deleted when nothing names
     * it, as it stands in the editor or as it lies on disk, and a resource
     * file only when no content names it. The files being written are checked
     * afresh, against resource directories scanned again for the save, and a
     * dirty file's disk content is read back from its file, so neither a stale
     * check nor a change not yet saved ever hides a dependency.
     */

    // Internal
    private InfoManager infoManager;
    private InfoLibraryBranch infoLibraryBranch;
    private InfoValidationBranch infoValidationBranch;
    private InfoReferenceBranch infoReferenceBranch;

    // Check — main thread only
    private ObjectOpenHashSet<InfoDocumentInstance> saving;
    private Object2ObjectOpenHashMap<String, ObjectOpenHashSet<String>> schemaName2ProjectedNames;

    // Base \\

    @Override
    protected void create() {

        // Check
        this.saving = new ObjectOpenHashSet<>();
        this.schemaName2ProjectedNames = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.infoLibraryBranch = get(InfoLibraryBranch.class);
        this.infoValidationBranch = get(InfoValidationBranch.class);
        this.infoReferenceBranch = get(InfoReferenceBranch.class);
    }

    // Save \\

    void beginSave(ObjectArrayList<InfoDocumentInstance> documents) {

        saving.clear();
        saving.addAll(documents);
        schemaName2ProjectedNames.clear();
    }

    void endSave() {

        saving.clear();
        schemaName2ProjectedNames.clear();
    }

    // Why one document of the set begun cannot be written with the rest, empty when it can
    String findSaveRefusal(InfoDocumentInstance document) {

        ObjectArrayList<InfoIssueStruct> issues = infoManager.refreshIssues(document);

        if (document.hasErrors())
            return describeProblems(document, issues);

        String unsaved = findUnsavedName(document);

        if (!unsaved.isEmpty())
            return unsaved;

        return findTakenName(document);
    }

    // A name the document holds that will not be on disk once the set is written
    private String findUnsavedName(InfoDocumentInstance document) {

        ObjectArrayList<InfoLinkStruct> links = collectLinks(document, document.getRoot());

        for (int i = 0; i < links.size(); i++) {

            InfoLinkStruct link = links.get(i);

            if (link.getKind() == InfoReferenceKind.SCHEMA
                    && !resolveProjectedNames(link.getTargetName()).contains(link.getName()))
                return EditorSetting.INFO_REFUSAL_PREFIX + document.getDefinitionName()
                        + EditorSetting.INFO_REFUSAL_NAMES + link.getName()
                        + EditorSetting.INFO_REFUSAL_IN + infoReferenceBranch.getTabName(link.getTargetName())
                        + EditorSetting.INFO_REFUSAL_NOT_SAVED;
        }

        return EditorSetting.INFO_REFUSAL_NONE;
    }

    // A name the document takes off disk that a file staying on disk as it is still names
    private String findTakenName(InfoDocumentInstance document) {

        InfoSchemaHandle schema = document.getSchema();
        ObjectOpenHashSet<String> taken = new ObjectOpenHashSet<>();

        infoReferenceBranch.addSavedNames(document, taken);
        taken.removeAll(resolveProjectedNames(schema.getSchemaName()));

        if (taken.isEmpty())
            return EditorSetting.INFO_REFUSAL_NONE;

        InfoLinkStruct link = findSavedReferrer(schema.getSchemaName(), taken);

        return link != null
                ? EditorSetting.INFO_REFUSAL_PREFIX + document.getDefinitionName()
                        + EditorSetting.INFO_REFUSAL_TAKES + link.getName()
                        + EditorSetting.INFO_REFUSAL_STILL_NAMED + describeLink(link)
                : EditorSetting.INFO_REFUSAL_NONE;
    }

    // A link on disk, from a file not being written, to one of these names
    private InfoLinkStruct findSavedReferrer(String schemaName, ObjectOpenHashSet<String> names) {

        ObjectArrayList<InfoSchemaHandle> referrers = infoManager.getReferrerSchemas(schemaName);

        for (int i = 0; i < referrers.size(); i++) {

            ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(referrers.get(i));

            for (int j = 0; j < documents.size(); j++) {

                InfoDocumentInstance referrer = documents.get(j);

                if (saving.contains(referrer) || !referrer.isOnDisk())
                    continue;

                InfoLinkStruct link = findLink(
                        collectLinks(referrer, resolveSavedRoot(referrer)), schemaName, names, null);

                if (link != null)
                    return link;
            }
        }

        return null;
    }

    private ObjectOpenHashSet<String> resolveProjectedNames(String schemaName) {

        ObjectOpenHashSet<String> names = schemaName2ProjectedNames.get(schemaName);

        if (names != null)
            return names;

        names = new ObjectOpenHashSet<>();
        ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(infoManager.getSchema(schemaName));

        for (int i = 0; i < documents.size(); i++) {

            InfoDocumentInstance document = documents.get(i);

            if (saving.contains(document))
                infoReferenceBranch.addLiveNames(document, names);
            else
                infoReferenceBranch.addSavedNames(document, names);
        }

        schemaName2ProjectedNames.put(schemaName, names);
        return names;
    }

    // Delete \\

    // Why a whole file, or one entry of it, cannot be deleted, empty when nothing names it
    String findDeleteRefusal(InfoDocumentInstance document, String entryName) {

        InfoSchemaHandle schema = document.getSchema();
        ObjectOpenHashSet<String> names = new ObjectOpenHashSet<>();

        if (entryName != null) {
            names.add(infoReferenceBranch.toReferenceName(schema, document.getDefinitionName(), entryName));
        } else {
            infoReferenceBranch.addLiveNames(document, names);
            infoReferenceBranch.addSavedNames(document, names);
        }

        String requiredName = findLastRequiredName(schema, names);

        if (requiredName != null)
            return EditorSetting.INFO_REFUSAL_DELETE_PREFIX + requiredName + EditorSetting.INFO_REFUSAL_REQUIRED;

        InfoLinkStruct link = findAnyReferrer(schema.getSchemaName(), names, document, entryName);

        return link != null
                ? EditorSetting.INFO_REFUSAL_DELETE_PREFIX + link.getName()
                        + EditorSetting.INFO_REFUSAL_STILL_NAMED + describeLink(link)
                : EditorSetting.INFO_REFUSAL_NONE;
    }

    // A name the game cannot start without that no other entry would hold once these names are deleted
    private String findLastRequiredName(InfoSchemaHandle schema, ObjectOpenHashSet<String> names) {

        ObjectArrayList<String> requiredNames = schema.getRequiredNames();

        for (int i = 0; i < requiredNames.size(); i++)
            if (names.contains(requiredNames.get(i))
                    && infoReferenceBranch.countName(schema.getSchemaName(), requiredNames.get(i)) <= 1)
                return requiredNames.get(i);

        return null;
    }

    // A link to one of these names, live or on disk, other than from the content being deleted itself
    private InfoLinkStruct findAnyReferrer(
            String schemaName,
            ObjectOpenHashSet<String> names,
            InfoDocumentInstance deleted,
            String deletedEntryName) {

        ObjectArrayList<InfoSchemaHandle> referrers = infoManager.getReferrerSchemas(schemaName);

        for (int i = 0; i < referrers.size(); i++) {

            ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(referrers.get(i));

            for (int j = 0; j < documents.size(); j++) {

                InfoDocumentInstance referrer = documents.get(j);

                if (referrer == deleted && deletedEntryName == null)
                    continue;

                String skippedEntryName = referrer == deleted ? deletedEntryName : null;
                InfoLinkStruct link = findLink(
                        collectLinks(referrer, referrer.getRoot()), schemaName, names, skippedEntryName);

                if (link == null && referrer.isOnDisk() && referrer.isDirty())
                    link = findLink(
                            collectLinks(referrer, resolveSavedRoot(referrer)), schemaName, names, skippedEntryName);

                if (link != null)
                    return link;
            }
        }

        return null;
    }

    // Resources \\

    // True when any content, live or on disk, names a file of a resource directory
    boolean isResourceNamed(String directory, String name) {

        ObjectArrayList<InfoSchemaHandle> referrers = infoManager.getResourceReferrerSchemas(directory);
        ObjectOpenHashSet<String> names = new ObjectOpenHashSet<>();

        names.add(name);

        for (int i = 0; i < referrers.size(); i++) {

            ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(referrers.get(i));

            for (int j = 0; j < documents.size(); j++) {

                InfoDocumentInstance referrer = documents.get(j);

                if (findLink(collectLinks(referrer, referrer.getRoot()), directory, names, null) != null)
                    return true;

                if (referrer.isOnDisk() && referrer.isDirty()
                        && findLink(collectLinks(referrer, resolveSavedRoot(referrer)), directory, names, null) != null)
                    return true;
            }
        }

        return false;
    }

    // Links \\

    private ObjectArrayList<InfoLinkStruct> collectLinks(InfoDocumentInstance document, ArpgObjectStruct root) {
        return root != null
                ? infoValidationBranch.collectLinks(document.getSchema(), document.getDefinitionName(), root)
                : new ObjectArrayList<>();
    }

    private InfoLinkStruct findLink(
            ObjectArrayList<InfoLinkStruct> links,
            String targetName,
            ObjectOpenHashSet<String> names,
            String skippedEntryName) {

        for (int i = 0; i < links.size(); i++) {

            InfoLinkStruct link = links.get(i);

            if (link.getTargetName().equals(targetName) && names.contains(link.getName())
                    && (skippedEntryName == null || !skippedEntryName.equals(link.getEntryName())))
                return link;
        }

        return null;
    }

    // A file's content as it lies on disk: its live tree while it holds no unsaved edit
    private ArpgObjectStruct resolveSavedRoot(InfoDocumentInstance document) {
        return document.isDirty()
                ? infoLibraryBranch.load(document.getSchema(), document.getDefinitionName())
                : document.getRoot();
    }

    // Text \\

    private String describeProblems(InfoDocumentInstance document, ObjectArrayList<InfoIssueStruct> issues) {

        InfoIssueStruct first = null;

        for (int i = 0; i < issues.size() && first == null; i++)
            if (issues.get(i).isError())
                first = issues.get(i);

        int errors = document.getErrorCount();

        return EditorSetting.INFO_REFUSAL_PREFIX + document.getDefinitionName()
                + EditorSetting.INFO_REFUSAL_HAS + errors
                + (errors == 1 ? EditorSetting.INFO_STATUS_PROBLEM : EditorSetting.INFO_STATUS_PROBLEMS)
                + infoManager.describeIssue(first);
    }

    private String describeLink(InfoLinkStruct link) {

        String location = link.getDefinitionName().equals(link.getEntryName())
                ? link.getDefinitionName()
                : link.getDefinitionName() + EditorSetting.INFO_TITLE_SEPARATOR + link.getEntryName();

        return infoReferenceBranch.getTabName(link.getSourceSchemaName()) + EditorSetting.INFO_TITLE_SEPARATOR
                + location + EditorSetting.INFO_REFUSAL_AT + infoManager.toDisplayPath(link.getPath());
    }
}
