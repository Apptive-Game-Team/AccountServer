# Super Admin Setup

To use the Server Token Issuance feature, you need to have SUPER_ADMIN authority.

## Setup Steps

1. Create a system for admin authorities (if not exists):
   ```sql
   INSERT INTO principal DEFAULT VALUES RETURNING id;
   -- Use the returned ID as principal_id below
   INSERT INTO system (principal_id, name, created_by) VALUES (<principal_id>, 'ADMIN', <your_member_id>);
   ```

2. Create SUPER_ADMIN authority:
   ```sql
   -- Get the system_id from the system you created
   INSERT INTO authority (system_id, value) VALUES (<system_id>, 'SUPER_ADMIN');
   ```

3. Grant SUPER_ADMIN authority to a member:
   ```sql
   -- Get authority_id from the authority you created and member_id of the user
   INSERT INTO member_authority (member_id, authority_id) VALUES (<member_id>, <authority_id>);
   ```

After these steps, the member will have SUPER_ADMIN authority and can access the token issuance page at `/admin/tokens`.

## Alternative: Use the Admin UI

You can also set this up using the admin interface:
1. Go to `/admin/systems` and create an "ADMIN" system
2. Go to `/admin/authorities` and create a "SUPER_ADMIN" authority for the ADMIN system
3. Go to `/admin/members/{member_id}` and grant the SUPER_ADMIN authority to the member
