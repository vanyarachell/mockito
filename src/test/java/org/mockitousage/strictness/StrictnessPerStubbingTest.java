package org.mockitousage.strictness;

import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.PotentialStubbingProblem;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.exceptions.verification.NoInteractionsWanted;
import org.mockito.quality.Strictness;
import org.mockitousage.IMethods;
import org.mockitoutil.TestBase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StrictnessPerStubbingTest {

    MockitoSession mockito;
    @Mock IMethods mock;

    @Before
    public void before() {
        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();
    }

    @Test
    public void potential_stubbing_problem() {
        //when
        when(mock.simpleMethod("1")).thenReturn("1");
        lenient().when(mock.differentMethod("2")).thenReturn("2");

        //then on lenient stubbing, we can call it with different argument:
        mock.differentMethod("200");

        //but on strict stubbing, we cannot:
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                mock.simpleMethod("100");
            }
        }).isInstanceOf(PotentialStubbingProblem.class);
    }

    @Test
    public void unnecessary_stubbing() {
        //when
        when(mock.simpleMethod("1")).thenReturn("1");
        lenient().when(mock.differentMethod("2")).thenReturn("2");

        //then unnecessary stubbing flags method only on the strict stubbing:
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                mockito.finishMocking();
            }
        }).isInstanceOf(UnnecessaryStubbingException.class)
            .hasMessageContaining("1. -> ")
            //good enough to prove that we're flagging just one unnecessary stubbing:
            //TODO: this assertion is duplicated with StrictnessPerMockTest
            .isNot(TestBase.hasMessageContaining("2. ->"));
    }

    @Test
    @Ignore("TODO")
    public void verify_no_more_invocations() {
        //when
        when(mock.simpleMethod("1")).thenReturn("1");
        lenient().when(mock.differentMethod("2")).thenReturn("2");

        //and:
        mock.simpleMethod("1");
        mock.differentMethod("200"); // <- different arg

        //then 'verifyNoMoreInteractions' ignores strict stub (implicitly verified) but flags the lenient stubbing (called with different arg)
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                verifyNoMoreInteractions(mock);
            }
        }).isInstanceOf(NoInteractionsWanted.class)
            .hasMessageContaining("But found this interaction on mock")
            //TODO: assertion duplicated with StrictnessPerMockTest
            .hasMessageContaining("Actually, above is the only interaction with this mock");
    }

    @After
    public void after() {
        mockito.finishMocking();
    }
}
