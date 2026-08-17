# Fix Build Path Error for UserPermissions

## Problem

Eclipse reports: "Cannot find the class file for cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions"

## Root Cause

The `cmkerp-shared-kernel` module has been built successfully via Maven, but Eclipse's build path is out of sync with Maven dependencies.

## Solution

### Option 1: Update Maven Project in Eclipse (Recommended)

1. Right-click on the `cmkerp-gateway` project in Eclipse Package Explorer
2. Select **Maven** → **Update Project...**
3. In the dialog:
   - Ensure `cmkerp-gateway` is selected
   - Check **"Force Update of Snapshots/Releases"**
   - Check **"Clean projects"** (optional but recommended)
4. Click **OK**
5. Wait for Eclipse to refresh the project and rebuild

### Option 2: Clean and Rebuild

1. Right-click on `cmkerp-gateway` project
2. Select **Project** → **Clean...**
3. Select `cmkerp-gateway` and click **OK**
4. Right-click on `cmkerp-gateway` project again
5. Select **Maven** → **Update Project...**
6. Click **OK**

### Option 3: Refresh All Projects

1. Right-click on the workspace root or parent `cmkerp` project
2. Select **Maven** → **Update Project...**
3. Select all modules (cmkerp-shared-kernel, cmkerp-gateway, etc.)
4. Check **"Force Update of Snapshots/Releases"**
5. Click **OK**

## Verification

After updating:

- The error should disappear from the Problems view
- The project should compile without errors
- You should be able to see `UserPermissions` class in the project dependencies

## Technical Details

- **Dependency Location**: `cmkerp-shared-kernel` module
- **Class Location**: `cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions`
- **Maven Dependency**: Already correctly configured in `pom.xml` (lines 88-99)
- **Build Status**: ✅ Maven build successful - the class file exists at:
  `cmkerp-shared-kernel/target/classes/cd/shad/erp/cmk/cmkerp/sharedkernel/security/UserPermissions.class`

## If Problem Persists

1. Close and reopen Eclipse
2. Delete `.classpath` and `.project` files (backup first), then re-import the project
3. Verify Maven is properly configured: **Window** → **Preferences** → **Maven** → **User Settings**


