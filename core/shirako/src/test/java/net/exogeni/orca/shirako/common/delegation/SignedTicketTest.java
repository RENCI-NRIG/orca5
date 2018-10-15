package net.exogeni.orca.shirako.common.delegation;

public class SignedTicketTest extends SimpleTicketTest
{
    public SignedTicketTest() throws Exception
    {
    }

    @Override
    protected IResourceTicketFactory makeTicketFactory()
    {
        return new SignedResourceTicketFactory();
    }
}
