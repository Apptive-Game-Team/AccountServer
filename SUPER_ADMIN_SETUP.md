# Super Admin Setup

The whole `/admin` console (and the Server Token Issuance feature) requires the
SUPER_ADMIN authority. The granted authority string is `<SYSTEM_NAME>_<AUTHORITY_VALUE>`
(see `Authority.getAuthority()`), so `SUPER_ADMIN` means system `SUPER` + authority `ADMIN`.
The first SUPER_ADMIN must be created with the SQL below — the admin UI is itself gated.

## Setup Steps

1. Create a system for admin authorities (if not exists):
   ```sql
   INSERT INTO principal DEFAULT VALUES RETURNING id;
   -- Use the returned ID as principal_id below
   INSERT INTO system (principal_id, name, created_by) VALUES (<principal_id>, 'SUPER', <your_member_id>);
   ```

2. Create SUPER_ADMIN authority:
   ```sql
   -- Get the system_id from the system you created
   INSERT INTO authority (system_id, value) VALUES (<system_id>, 'ADMIN');
   ```

3. Grant SUPER_ADMIN authority to a member:
   ```sql
   -- Get authority_id from the authority you created and member_id of the user
   INSERT INTO member_authority (member_id, authority_id) VALUES (<member_id>, <authority_id>);
   ```

After these steps, the member will have SUPER_ADMIN authority and can access the token issuance page at `/admin/tokens`.

## Alternative: Use the Admin UI

Only works if you already are a SUPER_ADMIN (the admin UI requires that authority):
1. Go to `/admin/systems` and create a "SUPER" system
2. Go to `/admin/authorities` and create an "ADMIN" authority for the SUPER system
3. Go to `/admin/members/{member_id}` and grant that authority to the member
